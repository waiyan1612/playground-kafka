package com.example.playground.kafka.demo.txn;

import com.example.playground.kafka.demo.ProducerHelper;

import java.io.IOException;
import java.net.URISyntaxException;

public class TxnTombstoneProducer {

    public static void main(String[] args) throws IOException, URISyntaxException {
        deleteFromJson();
    }

    public static void deleteFromJson() throws IOException, URISyntaxException {
        String jsonPayload = ProducerHelper.getJsonPayload("data/transactions-1.json");
        ProducerHelper.sendDeleteRequest("/txn", jsonPayload);
    }
}
