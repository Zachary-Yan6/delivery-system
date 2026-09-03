package com.zachary.delivery_system.mapper;

import com.zachary.delivery_system.entity.DriverLocationOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.UUID;

public interface DriverLocationOutboxMapper {

    @Insert("""
            INSERT INTO driver_location_outbox (
                id,
                event_type,
                aggregate_id,
                payload,
                status,
                attempt_count,
                next_attempt_at
            )
            VALUES (
                #{id},
                #{eventType},
                #{aggregateId},
                CAST(#{payload} AS JSONB),
                'PENDING',
                0,
                CURRENT_TIMESTAMP
            )
            """)
    int insertPending(
            @Param("id") UUID id,
            @Param("eventType") String eventType,
            @Param("aggregateId") Long aggregateId,
            @Param("payload") String payload
    );

    /**
     * Atomically claims a batch of events.
     * <p>
     * SKIP LOCKED means that multiple application instances can process
     * different events without waiting for each other.
     * <p>
     * Expired PROCESSING records are reclaimed after two minutes.
     */
    @Select(
            value = """
                    WITH candidates AS (
                        SELECT id
                        FROM driver_location_outbox
                        WHERE (
                            status = 'PENDING'
                            AND next_attempt_at <= CURRENT_TIMESTAMP
                        )
                        OR (
                            status = 'PROCESSING'
                            AND locked_until <= CURRENT_TIMESTAMP
                        )
                        ORDER BY created_at
                        LIMIT #{limit}
                    /**
                      lock selected data row and other waiting threaing would not wait
                     */
                        FOR UPDATE SKIP LOCKED
                    )
                    UPDATE driver_location_outbox AS outbox
                    SET
                        status = 'PROCESSING',
                        lock_token = #{lockToken},
                        locked_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes'
                    FROM candidates
                    WHERE outbox.id = candidates.id
                    RETURNING
                        outbox.id,
                        outbox.event_type AS "eventType",
                        outbox.aggregate_id AS "aggregateId",
                        outbox.payload,
                        outbox.status,
                        outbox.attempt_count AS "attemptCount",
                        outbox.next_attempt_at AS "nextAttemptAt",
                        outbox.lock_token AS "lockToken",
                        outbox.locked_until AS "lockedUntil",
                        outbox.published_at AS "publishedAt",
                        outbox.last_error AS "lastError",
                        outbox.created_at AS "createdAt"
                    """,
            affectData = true
    )
    List<DriverLocationOutbox> claimBatch(
            @Param("limit") int limit,
            @Param("lockToken") UUID lockToken
    );

    @Update("""
            UPDATE driver_location_outbox
            SET
                status = 'PUBLISHED',
                published_at = CURRENT_TIMESTAMP,
                lock_token = NULL,
                locked_until = NULL,
                last_error = NULL
            WHERE id = #{id}
              AND status = 'PROCESSING'
              AND lock_token = #{lockToken}
            """)
    int markPublished(
            @Param("id") UUID id,
            @Param("lockToken") UUID lockToken
    );

    @Update("""
            UPDATE driver_location_outbox
            SET
                status = 'PENDING',
                attempt_count = attempt_count + 1,
                next_attempt_at =
                    CURRENT_TIMESTAMP
                    + (
                        LEAST(
                            300,
                            CAST(
                                POWER(
                                    2,
                                    LEAST(attempt_count, 8)
                                ) AS INTEGER
                            )
                        ) * INTERVAL '1 second'
                    ),
                lock_token = NULL,
                locked_until = NULL,
                last_error = LEFT(#{lastError}, 2000)
            WHERE id = #{id}
              AND status = 'PROCESSING'
              AND lock_token = #{lockToken}
            """)
    int scheduleRetry(
            @Param("id") UUID id,
            @Param("lockToken") UUID lockToken,
            @Param("lastError") String lastError
    );
}