package com.example.expensechallenge.infrastructure.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer strictDeserializationCustomizer() {
        return builder -> builder
            // Reject JSON numbers where String is expected (purchaseAmountUsd must be "125.49" not 125.49)
            .withCoercionConfig(String.class, cfg -> {
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                cfg.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            })
            // Reject epoch integers for date fields (1746086400 → year +4782592 without this guard)
            .withCoercionConfig(LocalDate.class, cfg -> {
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                cfg.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            })
            // Replace the lenient LocalDate deserializer so datetime strings like
            // "2026-05-01T10:30:00" are rejected instead of silently truncated to the date part
            .addModule(new SimpleModule().addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE)));
    }
}
