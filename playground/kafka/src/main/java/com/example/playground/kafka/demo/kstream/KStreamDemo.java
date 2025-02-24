package com.example.playground.kafka.demo.kstream;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.demo.ConsumerHelper;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.model.TransactionXPayment;
import com.example.playground.kafka.serde.CustomJsonDeserializer;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class KStreamDemo {

    private static final Logger log = LoggerFactory.getLogger(KStreamDemo.class);

    private static final KafkaProperties kafkaProperties = new KafkaProperties();

    private static final Serde<Transaction> txnSerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Transaction.class)
    );

    private static final Serde<Payment> paySerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Payment.class)
    );

    private static final Serde<TransactionXPayment> txnXPaySerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(TransactionXPayment.class)
    );

    public static void main(String[] args) {

        // Adding a random suffix to the kafka consumer group name to ensure a fresh set of consumer offset.
        final boolean addRandomSuffix = true;

        // This will add processors that will peek and print to console in the topology.
        final boolean debug = true;

        String optSuffix = addRandomSuffix ? "-" + UUID.randomUUID() : "";
        log.info("{} will be appended to the application.id", optSuffix);

        Properties streamXStreamProps = new Properties();
        streamXStreamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "txn-pay-stream-x-stream" + optSuffix);
        streamXStreamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Transaction> txnStream = builder.stream(kafkaProperties.getTxnTopic(), Consumed.with(Serdes.String(), txnSerde));
        KStream<String, Payment> payStream = builder.stream(kafkaProperties.getPayTopic(), Consumed.with(Serdes.String(), paySerde));

        int windowSize = 20;
        int advanceSize = 5;
        // check https://kafka.apache.org/20/documentation/streams/developer-guide/dsl-api.html#streams-developer-guide-dsl-joins-co-partitioning
        TimeWindows tumblingWindow = TimeWindows.ofSizeAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0));
        TimeWindows hoppingWindow = TimeWindows.ofSizeAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0)).advanceBy(Duration.ofSeconds(advanceSize));

        KStream<String, TransactionXPayment> joinedStream = txnStream.outerJoin(
                payStream,
                TransactionXPayment::fromTransactionAndPayment,
                JoinWindows.ofTimeDifferenceAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0)),
                StreamJoined.with(Serdes.String(), txnSerde, paySerde)
        );
        if (debug) {
            joinedStream.peek((key, value) -> log.info("streamXstream: joinedTbl contents: {}:{}", key, value));
        }

        Topology streamTopology =  builder.build();

        // Can use open-source tools like
        // https://zz85.github.io/kafka-streams-viz/
        log.info("Topology: {}", streamTopology.describe());

        // Build streams from the topology
        final CountDownLatch latch = new CountDownLatch(3);
        try (KafkaStreams streams = ConsumerHelper.constructStreams(streamTopology, streamXStreamProps, latch)) {
            try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
                log.info("Starting threads in the background ...");
                // This is to start the kafka stream and keep it running.
                executorService.submit(() -> ConsumerHelper.startKafkaStreams(streams, latch));
                // Shutdown hook to clean up
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    executorService.shutdown();
                    log.info("Shutting down executor service...");
                }));
            }
        }
    }
}
