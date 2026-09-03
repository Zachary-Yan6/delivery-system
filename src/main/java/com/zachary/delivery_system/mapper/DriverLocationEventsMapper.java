package com.zachary.delivery_system.mapper;

import com.zachary.delivery_system.dto.Location.DriverStaleLocationCandidate;
import com.zachary.delivery_system.projection.analytics.DriverLocationActivityProjection;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public interface DriverLocationEventsMapper {

    @Insert("""
            INSERT INTO driver_location_events (
                received_at,
                event_id,
                driver_id,
                latitude,
                longitude,
                recorded_at
            )
            VALUES (
                #{receivedAt},
                #{eventId},
                #{driverId},
                #{latitude},
                #{longitude},
                #{recordedAt}
            )
            ON CONFLICT (received_at, event_id) DO NOTHING
            """)
    int insertIgnore(
            @Param("receivedAt") Instant receivedAt,
            @Param("eventId") UUID eventId,
            @Param("driverId") Long driverId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("recordedAt") Instant recordedAt
    );

    @Select("""
        SELECT
            e.driver_id AS "driverId",
            d.full_name AS "driverName",
            COUNT(*) AS "locationCount",
            MAX(e.received_at) AS "lastReceivedAt"
        FROM driver_location_events e
        JOIN drivers d ON d.id = e.driver_id
        WHERE e.received_at >= #{since}
        GROUP BY e.driver_id, d.full_name
        ORDER BY "locationCount" DESC, "driverId"
        """)
    List<DriverLocationActivityProjection> selectActivitySince(
            @Param("since") Instant since
    );

    @Select("""
        SELECT
            e.driver_id AS "driverId",
            d.full_name AS "driverName",
            MAX(e.received_at) AS "lastReceivedAt"
        FROM driver_location_events e
        JOIN drivers d ON d.id = e.driver_id
        WHERE d.active = TRUE
        GROUP BY e.driver_id, d.full_name
        HAVING MAX(e.received_at) < #{cutoff}
        """)
    List<DriverStaleLocationCandidate> selectDriversWithStaleLocation(
            @Param("cutoff") Instant cutoff
    );
}
