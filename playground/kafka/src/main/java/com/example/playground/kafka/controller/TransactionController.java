package com.example.playground.kafka.controller;

import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.producer.TransactionProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/txn")
public class TransactionController {

    private final TransactionProducer transactionProducer;
    private final Random random;

    @Autowired
    public TransactionController(TransactionProducer transactionProducer) {
        this.transactionProducer = transactionProducer;
        this.random = new Random();
    }

    @PostMapping
    public void post(@RequestBody List<Transaction> transactions) {
        transactionProducer.send(transactions);
    }

    @DeleteMapping
    public void delete(@RequestBody List<Transaction> transactions) {
        transactionProducer.delete(transactions);
    }

    @PostMapping("/random")
    public void postRandom() {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        int totalTransactions = random.ints(0, 10).findFirst().getAsInt();
        List<Transaction> transactions = new ArrayList<>(totalTransactions);
        for(int i=0; i<totalTransactions; i++) {
            transactions.add(new Transaction(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                BigDecimal.valueOf(random.nextDouble()),
                BigDecimal.valueOf(random.nextDouble()),
                LocalDateTime.now()
            ));
        }
        transactionProducer.send(transactions);
    }
}
