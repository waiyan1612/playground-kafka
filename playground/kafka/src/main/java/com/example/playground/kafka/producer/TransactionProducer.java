package com.example.playground.kafka.producer;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.serde.DefaultSerializer;
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
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final Producer<String, Transaction> producer;
    private final String txnTopic;

    @Autowired
    public TransactionProducer(KafkaProperties kafkaProperties) {
        txnTopic = kafkaProperties.getTxnTopic();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getServers());
        properties.put("client.id", "transaction-producer@playground");
        properties.put("key.serializer", StringSerializer.class);
        properties.put("value.serializer", DefaultSerializer.class);
        producer = new KafkaProducer<>(properties);
    }

    public void send(List<Transaction> transactions) {
        transactions.forEach(this::send);
    }

    private void send(Transaction txn) {
        try {
            ProducerRecord<String, Transaction> producerRecord = new ProducerRecord<>(txnTopic, txn.id(), txn);
            RecordMetadata metadata = producer.send(producerRecord).get();
            log.info("Record {} sent to partition {} with offset {}",
                    txn, metadata.partition(), metadata.offset());
        } catch (ExecutionException e) {
            log.error("Error in sending record: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Interrupted :{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
