package com.example.playground.kafka.controller;

import com.example.playground.kafka.model.Customer;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.producer.CustomerProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerProducer customerProducer;
    private final Random random;

    @Autowired
    public CustomerController(CustomerProducer customerProducer) {
        this.customerProducer = customerProducer;
        this.random = new Random();
    }

    @PostMapping
    public void post(@RequestBody List<Customer> customers) {
        customerProducer.send(customers);
    }

    @DeleteMapping
    public void delete(@RequestBody List<Customer> customers) {
        customerProducer.delete(customers);
    }

    @PostMapping("/random")
    public void postRandom() {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        int totalPayments = random.ints(0, 10).findFirst().getAsInt();

        List<Customer> customers = new ArrayList<>(totalPayments);
        for(int i=0; i<totalPayments; i++) {
            customers.add(new Customer(
                "Customer_XYZ",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            ));
        }
        customerProducer.send(customers);
    }
}
