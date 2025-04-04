package com.example.playground.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionXPaymentXCustomer(
    String transactionId,
    String paymentId,
    String customerId,
    String paymentStatus,
    String displayName
) {
    public static TransactionXPaymentXCustomer fromTransactionXPaymentAndCustomer(TransactionXPayment txnxpay, Customer cust) {
        String transactionId = null;
        String paymentId = null;
        String customerId = null;
        String paymentStatus = null;
        String displayName = null;
        if (cust != null) {
            displayName = cust.displayName();
            customerId = txnxpay.customerId();
        }
        if (txnxpay != null) {
            transactionId = txnxpay.transactionId();
            paymentId = txnxpay.paymentId();
            customerId = txnxpay.customerId();
            paymentStatus = txnxpay.paymentStatus();
        }
        return new TransactionXPaymentXCustomer(transactionId, paymentId, customerId, paymentStatus, displayName);
    }

    public boolean missingTxnPay() {
        return transactionId == null;
    }

    public boolean missingCustomer() {
        return displayName == null;
    }

    public boolean isComplete() {
        return !missingTxnPay() && !missingCustomer();
    }
}
