package com.example.playground.kafka.demo.txnxpay;

import com.example.playground.kafka.demo.ProducerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class TxnXPayProducer {

    private static final Logger log = LoggerFactory.getLogger(TxnXPayProducer.class);

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        produceFromJson();
//        produceRandom();
    }

    public static void produceRandom() {
        ProducerHelper.sendPostRequest("/txn/random", "");
        ProducerHelper.sendPostRequest("/pay/random", "");
    }

    public static void produceFromJson() throws IOException, URISyntaxException, InterruptedException {

        // Send all transactions at 00:00
        int sleepSeconds = 0;
        String txnPayload = ProducerHelper.getJsonPayload("data/transactions.json");
        ProducerHelper.sendPostRequest("/txn", txnPayload);
        log.info("Sent txn payload");

        // Send payment 1A at 00:10
        sleepSeconds = 10;
        log.info("Sleeping {} seconds", sleepSeconds);
        Thread.sleep(sleepSeconds * 1000L);

        ProducerHelper.sendPostRequest("/pay", ProducerHelper.getJsonPayload("data/payments-1a.json"));
        log.info("Sent payment 1a");

        // Send payment 1B and 2 at 00:30
        sleepSeconds = 20;
        log.info("Sleeping {} seconds", sleepSeconds);
        Thread.sleep(sleepSeconds * 1000L);

        ProducerHelper.sendPostRequest("/pay", ProducerHelper.getJsonPayload("data/payments-1b.json"));
        ProducerHelper.sendPostRequest("/pay", ProducerHelper.getJsonPayload("data/payments-2.json"));
        log.info("Sent payment 1b and 2");
    }
}
