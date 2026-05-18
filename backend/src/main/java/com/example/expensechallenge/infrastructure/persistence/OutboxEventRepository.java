package com.example.expensechallenge.infrastructure.persistence;

import com.example.expensechallenge.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends CrudRepository<OutboxEvent, UUID> {

    @Query("""
        SELECT * FROM outbox_events
        WHERE status = 'PENDING'
        ORDER BY created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """)
    List<OutboxEvent> findPendingBatch(@Param("limit") int limit);
}
