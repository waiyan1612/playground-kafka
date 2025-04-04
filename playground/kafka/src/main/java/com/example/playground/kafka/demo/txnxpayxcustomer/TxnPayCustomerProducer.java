package com.example.playground.kafka.demo.txnxpayxcustomer;

import com.example.playground.kafka.demo.ProducerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class TxnPayCustomerProducer {

    private static final Logger log = LoggerFactory.getLogger(TxnPayCustomerProducer.class);

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        produceFromJson();
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

        sleepSeconds = 40;
        Thread.sleep(sleepSeconds * 1000L);
        ProducerHelper.sendPostRequest("/customer", ProducerHelper.getJsonPayload("data/customers-1.json"));
        log.info("Sent customers 1");

        Thread.sleep(sleepSeconds * 1000L);
        ProducerHelper.sendPostRequest("/customer", ProducerHelper.getJsonPayload("data/customers-2.json"));
        log.info("Sent customers 2");
    }
}
