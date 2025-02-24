package com.example.playground.kafka.demo.txn;

import com.example.playground.kafka.demo.ProducerHelper;

import java.io.*;
import java.net.URISyntaxException;

public class TxnProducer {

    public static void main(String[] args) throws IOException, URISyntaxException {
        produceFromJson();
        produceRandom();
    }

    public static void produceRandom() {
        ProducerHelper.sendPostRequest("/txn/random", "");
    }

    public static void produceFromJson() throws IOException, URISyntaxException {
        String jsonPayload = ProducerHelper.getJsonPayload("data/transactions.json");
        ProducerHelper.sendPostRequest("/txn", jsonPayload);
    }
}
