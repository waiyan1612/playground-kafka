package com.example.playground.kafka.controller;

import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.producer.PaymentProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/pay")
public class PaymentController {

    private final PaymentProducer paymentProducer;
    private final Random random;

    @Autowired
    public PaymentController(PaymentProducer paymentProducer) {
        this.paymentProducer = paymentProducer;
        this.random = new Random();
    }

    @PostMapping
    public void post(@RequestBody List<Payment> payments) {
        paymentProducer.send(payments);
    }

    @DeleteMapping
    public void delete(@RequestBody List<Payment> payments) {
        paymentProducer.delete(payments);
    }

    @PostMapping("/random")
    public void postRandom() {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        int totalPayments = random.ints(0, 10).findFirst().getAsInt();

        List<Payment> payments = new ArrayList<>(totalPayments);
        for(int i=0; i<totalPayments; i++) {
            payments.add(new Payment(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "REJECTED",
                "AMEX",
                BigDecimal.valueOf(random.nextDouble()),
                LocalDateTime.now()
            ));
        }
        paymentProducer.send(payments);
    }
}
