package com.example.playground.kafka.demo.pay;

import com.example.playground.kafka.demo.ProducerHelper;

import java.io.IOException;
import java.net.URISyntaxException;

public class PayTombstoneProducer {

    public static void main(String[] args) throws IOException, URISyntaxException {
        deleteFromJson();
    }

    public static void deleteFromJson() throws IOException, URISyntaxException {
        String jsonPayload = ProducerHelper.getJsonPayload("data/payments-1a.json");
        ProducerHelper.sendDeleteRequest("/pay", jsonPayload);
    }
}
