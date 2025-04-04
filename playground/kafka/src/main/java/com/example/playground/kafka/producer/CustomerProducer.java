package com.example.playground.kafka.producer;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Customer;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
public class CustomerProducer {

    private static final Logger log = LoggerFactory.getLogger(CustomerProducer.class);


    private final Producer<String, Customer> producer;
    private final String customerTopic;

    @Autowired
    public CustomerProducer(KafkaProperties kafkaProperties) {
        customerTopic = kafkaProperties.getCustomerTopic();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getServers());
        properties.put("client.id", "customer-producer@playground");
        properties.put("key.serializer", StringSerializer.class);
        properties.put("value.serializer", CustomJsonSerializer.class);
        producer = new KafkaProducer<>(properties);
    }

    public void send(List<Customer> payments) {
        payments.forEach(this::send);
    }

    public void delete(List<Customer> payments) {
        payments.forEach(this::delete);
    }

    private void send(Customer customer) {
        try {
            // NOTE: MESSAGE KEY is transactionId, not paymentId. This is because for stream x stream joins,
            // we need the records we want to join to go into the same partition.
            // If we use paymentId as the key here, later we will need to repartition and stream
            // to a tmp / pass-through topic for the joins to read from.
            ProducerRecord<String, Customer> producerRecord = new ProducerRecord<>(customerTopic, customer.id(), customer);
            RecordMetadata metadata = producer.send(producerRecord).get();
            log.info("Record {} sent to partition {} with offset {}",
                    customer, metadata.partition(), metadata.offset());
        } catch (ExecutionException e) {
            log.error("Error in sending record: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Interrupted :{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    public void delete(Customer customer) {
        try {
            // Send null value for a given key,
            ProducerRecord<String, Customer> customerRecord = new ProducerRecord<>(customerTopic, customer.id(), null);
            RecordMetadata metadata = producer.send(customerRecord).get();
            log.info("Null Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());
        } catch (ExecutionException e) {
            log.error("Error in sending record: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Interrupted :{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
