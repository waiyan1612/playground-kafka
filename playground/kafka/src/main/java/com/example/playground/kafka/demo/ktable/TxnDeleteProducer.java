package com.example.playground.kafka.demo.ktable;

import com.example.playground.kafka.demo.ProducerHelper;

import java.io.IOException;
import java.net.URISyntaxException;

public class TxnDeleteProducer {

    public static void main(String[] args) throws IOException, URISyntaxException {
        deleteFromJson();
    }

    public static void deleteFromJson() throws IOException, URISyntaxException {
        String jsonPayload = ProducerHelper.getJsonPayload("data/transactions-1.json");
        ProducerHelper.sendDeleteRequest("/txn", jsonPayload);
    }
}
