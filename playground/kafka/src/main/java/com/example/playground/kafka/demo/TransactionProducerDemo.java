package com.example.playground.kafka.demo;

import java.io.*;
import java.net.URISyntaxException;

public class TransactionProducerDemo {

    public static void main(String[] args) throws IOException, URISyntaxException {
        produceFromJson();
        produceRandom();
    }

    public static void produceRandom() {
        DemoHelper.sendPostRequest("/txn/random", "");
    }

    public static void produceFromJson() throws IOException, URISyntaxException {
        String jsonPayload = DemoHelper.getJsonPayload("data/transactions.json");
        DemoHelper.sendPostRequest("/txn", jsonPayload);
    }
}
