package com.example.playground.kafka.demo.txn;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.serde.CustomJsonDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static com.example.playground.kafka.serde.CustomJsonDeserializer.CUSTOM_VALUE_DESERIALIZER_TYPE;


public class TxnConsumer {

    private static final Logger log = LoggerFactory.getLogger(TxnConsumer.class);

    public static void main(String[] args) {
        KafkaProperties kafkaProperties = new KafkaProperties();
        String servers = kafkaProperties.getServers();
        String txnTopic = kafkaProperties.getTxnTopic();
        List<String> topics = List.of(txnTopic);
        log.info("Bootstrap servers: {}", servers);
        log.info("Topics to subscribe: {}", topics);

        Properties properties = new Properties();
        properties.put("bootstrap.servers", servers);
        properties.put("client.id", "transaction-consumer@playground");
        properties.put("group.id", "transaction-consumer@playground89kjgiugbvjukg");
        properties.put("key.deserializer", StringDeserializer.class);
        properties.put("value.deserializer", CustomJsonDeserializer.class);
        properties.put("auto.offset.reset", "earliest");
        properties.put(CUSTOM_VALUE_DESERIALIZER_TYPE, Transaction.class);

        try (Consumer<String, Transaction> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(topics);
            while (true) {
                try {
                    ConsumerRecords<String, Transaction> kafkaRecords = consumer.poll(Duration.ofSeconds(1));
                    kafkaRecords.forEach(kafkaRecord -> log.info("Record {} received from partition {} with offset {}", kafkaRecord.value(), kafkaRecord.partition(), kafkaRecord.offset()));
                    consumer.commitAsync();
                } catch (RuntimeException e) {
                    log.error(e.getMessage(), e);
                    break;
                }
            }
        }
    }
}
