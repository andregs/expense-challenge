package com.example.expensechallenge.infrastructure.persistence;

import com.example.expensechallenge.domain.OutboxEvent;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface OutboxEventRepository extends CrudRepository<OutboxEvent, UUID> {
}
