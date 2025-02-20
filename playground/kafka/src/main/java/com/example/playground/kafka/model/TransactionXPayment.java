package com.example.playground.kafka.model;

public record TransactionXPayment(
    String transactionId,
    String paymentId,
    String customerId,
    String paymentStatus
) {
    public boolean missingPay() {
        return paymentId == null;
    }

    public boolean missingTxn() {
        return transactionId == null;
    }

    public boolean isComplete() {
        return !missingPay() && !missingTxn();
    }
}
