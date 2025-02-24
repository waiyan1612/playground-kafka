package com.example.playground.kafka.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
    String id,
    String customerId,
    String productId,
    BigDecimal units,
    BigDecimal costPerUnit,
    LocalDateTime transactionDate
) {}
