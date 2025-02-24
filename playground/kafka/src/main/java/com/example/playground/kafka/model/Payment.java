package com.example.playground.kafka.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(
    String id,
    String transactionId,
    String status,
    String mode,
    BigDecimal amount,
    LocalDateTime paymentDate
) {}
