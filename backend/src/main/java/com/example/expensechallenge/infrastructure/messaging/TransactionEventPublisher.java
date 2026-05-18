package com.example.expensechallenge.infrastructure.messaging;

import com.example.expensechallenge.domain.OutboxEvent;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, String>> publish(OutboxEvent event) {
        return kafkaTemplate.send(KafkaConfig.TOPIC, event.aggregateId().toString(), event.payload());
    }
}
