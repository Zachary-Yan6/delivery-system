package com.zachary.delivery_system.service.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.enums.DeliveryPriority;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryPickupMissingException;
import com.zachary.delivery_system.exception.DeliveryTimeWindowExpiredException;
import com.zachary.delivery_system.exception.NoEligibleDriverException;
import com.zachary.delivery_system.mapper.DeliveryMapper;
import com.zachary.delivery_system.mapper.DriverMapper;
import com.zachary.delivery_system.service.DriverLocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DriverAssignmentService {

    /*
     * Hard rules remove drivers who are inactive, unavailable, stale,
     * overloaded, or over vehicle capacity. The remaining driver with the
     * lowest score wins. Priority and a close time-window deadline increase
     * the importance of distance, while a future window reduces it.
     */

    private static final double EARTH_RADIUS_KM = 6_371.0;
    private static final double WORKLOAD_PENALTY_PER_DELIVERY = 5.0;
    private static final double CAPACITY_UTILIZATION_PENALTY = 5.0;
    private static final BigDecimal ZERO_WEIGHT = BigDecimal.ZERO;

    private final DriverMapper driverMapper;
    private final DeliveryMapper deliveryMapper;
    private final DriverLocationService driverLocationService;
    private final int maxActiveDeliveries;
    private final long maxLocationAgeSeconds;

    public DriverAssignmentService(
            DriverMapper driverMapper,
            DeliveryMapper deliveryMapper,
            DriverLocationService driverLocationService,
            @Value("${app.assignment.max-active-deliveries:10}")
            int maxActiveDeliveries,
            @Value("${app.assignment.max-location-age-seconds:300}")
            long maxLocationAgeSeconds
    ) {
        this.driverMapper = driverMapper;
        this.deliveryMapper = deliveryMapper;
        this.driverLocationService = driverLocationService;
        this.maxActiveDeliveries = maxActiveDeliveries;
        this.maxLocationAgeSeconds = maxLocationAgeSeconds;
    }

    public DriverAssignmentDecision selectBestDriver(Delivery delivery) {
        validateDeliveryCanBeScored(delivery);

        List<Driver> availableDrivers = driverMapper.selectList(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getActive, true)
                        .eq(Driver::getAvailable, true)
        );

        if (availableDrivers.isEmpty()) {
            throw new NoEligibleDriverException();
        }

        Map<Long, DriverLatestLocationResponse> locationsByDriver =
                latestUsableLocations();
        Map<Long, Workload> workloadsByDriver = activeWorkloads();

        return availableDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getActive()))
                .filter(driver -> Boolean.TRUE.equals(driver.getAvailable()))
                .filter(driver -> locationsByDriver.containsKey(driver.getId()))
                .map(driver -> score(
                        driver,
                        delivery,
                        locationsByDriver.get(driver.getId()),
                        workloadsByDriver.getOrDefault(
                                driver.getId(),
                                Workload.empty()
                        )
                ))
                .filter(decision -> decision != null)
                .min(
                        Comparator.comparingDouble(DriverAssignmentDecision::score)
                                .thenComparing(decision -> decision.driver().getId())
                )
                .orElseThrow(NoEligibleDriverException::new);
    }

    private void validateDeliveryCanBeScored(Delivery delivery) {
        if (delivery.getPickupLatitude() == null
                || delivery.getPickupLongitude() == null) {
            throw new DeliveryPickupMissingException(delivery.getId());
        }

        Date windowEnd = delivery.getTimeWindowEnd();
        if (windowEnd != null && !windowEnd.toInstant().isAfter(Instant.now())) {
            throw new DeliveryTimeWindowExpiredException(delivery.getId());
        }
    }

    private Map<Long, DriverLatestLocationResponse> latestUsableLocations() {
        Instant oldestAllowed = Instant.now()
                .minusSeconds(maxLocationAgeSeconds);

        return driverLocationService.getLatestLocations().stream()
                .filter(location -> location.getDriverId() != null)
                .filter(location -> location.getReceivedAt() != null)
                .filter(location -> location.getLatitude() != null)
                .filter(location -> location.getLongitude() != null)
                .filter(location -> location.getReceivedAt()
                        .toInstant()
                        .isAfter(oldestAllowed))
                .collect(Collectors.toMap(
                        DriverLatestLocationResponse::getDriverId,
                        Function.identity(),
                        (first, second) -> first.getReceivedAt()
                                .after(second.getReceivedAt())
                                ? first
                                : second
                ));
    }

    private Map<Long, Workload> activeWorkloads() {
        List<Delivery> activeDeliveries = deliveryMapper.selectList(
                new LambdaQueryWrapper<Delivery>()
                        .isNotNull(Delivery::getDriverId)
                        .in(
                                Delivery::getStatus,
                                DeliveryStatus.driverWorkloadStatuses()
                        )
        );

        Map<Long, Workload> workloads = new HashMap<>();

        for (Delivery activeDelivery : activeDeliveries) {
            workloads.compute(
                    activeDelivery.getDriverId(),
                    (driverId, current) -> (current == null
                            ? Workload.empty()
                            : current).add(weightOf(activeDelivery))
            );
        }

        return workloads;
    }

    private DriverAssignmentDecision score(
            Driver driver,
            Delivery delivery,
            DriverLatestLocationResponse location,
            Workload workload
    ) {
        if (workload.activeDeliveryCount() >= maxActiveDeliveries) {
            return null;
        }

        BigDecimal capacity = driver.getVehicleCapacityKg();
        if (capacity == null || capacity.signum() <= 0) {
            return null;
        }

        BigDecimal loadAfterAssignment = workload.activeLoadKg()
                .add(weightOf(delivery));

        if (loadAfterAssignment.compareTo(capacity) > 0) {
            return null;
        }

        double distanceKm = haversineDistanceKm(
                location.getLatitude().doubleValue(),
                location.getLongitude().doubleValue(),
                delivery.getPickupLatitude().doubleValue(),
                delivery.getPickupLongitude().doubleValue()
        );

        DeliveryPriority priority = delivery.getPriority() == null
                ? DeliveryPriority.NORMAL
                : delivery.getPriority();

        double urgencyWeight = priority.getDistanceWeight()
                * timeWindowUrgency(
                        delivery.getTimeWindowStart(),
                        delivery.getTimeWindowEnd()
                );
        double workloadPenalty = workload.activeDeliveryCount()
                * WORKLOAD_PENALTY_PER_DELIVERY;
        double capacityUtilization = loadAfterAssignment
                .divide(capacity, 6, RoundingMode.HALF_UP)
                .doubleValue();

        double score = distanceKm * urgencyWeight
                + workloadPenalty
                + capacityUtilization * CAPACITY_UTILIZATION_PENALTY;

        return new DriverAssignmentDecision(
                driver,
                score,
                distanceKm,
                workload.activeDeliveryCount(),
                workload.activeLoadKg()
        );
    }

    private BigDecimal weightOf(Delivery delivery) {
        return delivery.getPackageWeightKg() == null
                ? ZERO_WEIGHT
                : delivery.getPackageWeightKg();
    }

    private double timeWindowUrgency(
            Date windowStart,
            Date windowEnd
    ) {
        if (windowEnd == null) {
            return 1.0;
        }

        Instant now = Instant.now();

        if (windowStart != null
                && windowStart.toInstant().isAfter(now.plusSeconds(4 * 60 * 60))) {
            return 0.75;
        }

        long minutesRemaining = Duration.between(
                now,
                windowEnd.toInstant()
        ).toMinutes();

        if (minutesRemaining <= 60) {
            return 2.0;
        }

        if (minutesRemaining <= 240) {
            return 1.5;
        }

        return 1.0;
    }

    private double haversineDistanceKm(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        double latitudeDistance = Math.toRadians(
                endLatitude - startLatitude
        );
        double longitudeDistance = Math.toRadians(
                endLongitude - startLongitude
        );

        double value = Math.sin(latitudeDistance / 2)
                * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(startLatitude))
                * Math.cos(Math.toRadians(endLatitude))
                * Math.sin(longitudeDistance / 2)
                * Math.sin(longitudeDistance / 2);

        return EARTH_RADIUS_KM
                * 2
                * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private record Workload(
            int activeDeliveryCount,
            BigDecimal activeLoadKg
    ) {
        private static Workload empty() {
            return new Workload(0, ZERO_WEIGHT);
        }

        private Workload add(BigDecimal packageWeightKg) {
            return new Workload(
                    activeDeliveryCount + 1,
                    activeLoadKg.add(packageWeightKg)
            );
        }
    }
}
