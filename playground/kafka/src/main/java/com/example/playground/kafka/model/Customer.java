package com.example.playground.kafka.model;

public record Customer(
    String id,
    String displayName,
    String shippingAddress
) {}
