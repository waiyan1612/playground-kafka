package com.example.playground.kafka.demo;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.serde.CustomJsonDeserializer;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class TransactionNPaymentConsumerDemo {

    private static final Logger log = LoggerFactory.getLogger(TransactionNPaymentConsumerDemo.class);

    private static final KafkaProperties kafkaProperties = new KafkaProperties();

    private static final Serde<Transaction> txnSerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Transaction.class)
    );

    private static final Serde<Payment> paySerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Payment.class)
    );

    public static void main(String[] args) {
        String servers = kafkaProperties.getServers();
        String txnTopic = kafkaProperties.getTxnTopic();
        String payTopic = kafkaProperties.getPayTopic();
        List<String> topics = List.of(txnTopic, payTopic);
        log.info("Bootstrap servers: {}", servers);
        log.info("Topics to subscribe: {}", topics);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Transaction> txnStream = builder.stream("txn", Consumed.with(Serdes.String(), txnSerde));
        KStream<String, Payment> payStream = builder.stream("pay", Consumed.with(Serdes.String(), paySerde));

        // Debug prints to peek into stream. Non-terminal op.
        txnStream.peek((key, value) -> log.info("txnStream contents: {}:{}", key, value));
        payStream.peek((key, value) -> log.info("payStream contents: {}:{}", key, value));

        // Define a Join Window of 5 minutes
        JoinWindows joinWindows = JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5));

        KStream<String, String> joinedStream = txnStream.join(
            payStream,
            TransactionNPaymentConsumerDemo::joinTxnPay,
            joinWindows,
            StreamJoined.with(Serdes.String(), txnSerde, paySerde)
        );
        // Debug prints to peek into stream. Non-terminal op.
        joinedStream.peek((key, value) -> log.info("joinedStream contents: {}:{}", key, value));

        // Build and start the Kafka Streams application
        Topology streamTopology = builder.build();
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "transaction-payment-consumer");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        runKafkaStreams(new KafkaStreams(streamTopology, properties));
    }

    static String joinTxnPay(Transaction txn, Payment pay) {
        return String.format(" %s | %s | %s", txn.customerId(), txn.productId(), pay.status());
    }

    // From https://developer.confluent.io/tutorials/creating-first-apache-kafka-streams-application/confluent.html
    static void runKafkaStreams(final KafkaStreams streams) {
        final CountDownLatch latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });

        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        try {
            latch.await();
            log.warn("Latch over");
        } catch (final InterruptedException e) {
            log.warn("Application interrupted. Shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
