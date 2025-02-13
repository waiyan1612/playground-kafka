package com.example.playground.kafka.demo;

import java.io.IOException;
import java.net.URISyntaxException;

public class TransactionNPaymentProducerDemo {

    public static void main(String[] args) throws IOException, URISyntaxException {
        produceFromJson();
        produceRandom();
    }

    public static void produceRandom() {
        DemoHelper.sendPostRequest("/txn/random", "");
        DemoHelper.sendPostRequest("/pay/random", "");
    }

    public static void produceFromJson() throws IOException, URISyntaxException {
        String txnPayload = DemoHelper.getJsonPayload("data/transactions.json");
        DemoHelper.sendPostRequest("/txn", txnPayload);

        String payPayload = DemoHelper.getJsonPayload("data/payments.json");
        DemoHelper.sendPostRequest("/pay", payPayload);
    }
}
