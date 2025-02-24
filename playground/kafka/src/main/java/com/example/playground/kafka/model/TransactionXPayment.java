package com.example.playground.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionXPayment(
    String transactionId,
    String paymentId,
    String customerId,
    String paymentStatus
) {
    public static TransactionXPayment fromTransactionAndPayment(Transaction txn, Payment pay) {
        String transactionId = null;
        String paymentId = null;
        String customerId = null;
        String paymentStatus = null;
        if (pay != null) {
            paymentId = pay.id();
            paymentStatus = pay.status();
        }
        if (txn != null) {
            transactionId = txn.id();
            customerId = txn.customerId();
        }
        return new TransactionXPayment(transactionId, paymentId, customerId, paymentStatus);
    }

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
