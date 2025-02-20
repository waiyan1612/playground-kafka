package com.example.playground.kafka.demo;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.model.TransactionXPayment;
import com.example.playground.kafka.serde.CustomJsonDeserializer;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    private static final Serde<TransactionXPayment> txnXPaySerde = Serdes.serdeFrom(
            new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(TransactionXPayment.class)
    );

    public static void main(String[] args) {

        boolean addRandomSuffix = true;
        String optSuffix = addRandomSuffix ? "-" + UUID.randomUUID() : "";
        optSuffix = "-9f800929-9153-443e-9f88-a25c078f19b1";
        log.info("{} will be appended to the application.id", optSuffix);

        Properties streamXStreamProps = new Properties();
        streamXStreamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "txn-pay-stream-x-stream" + optSuffix);
        streamXStreamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());

        Properties tableXTableProps = new Properties();
        tableXTableProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "txn-pay-table-x-table" + optSuffix);
        tableXTableProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());

        Properties streamXTableProps = new Properties();
        streamXTableProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "txn-pay-stream-x-table" + optSuffix);
        streamXTableProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());

        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            log.info("Starting threads in the background ...");
            executorService.submit(() -> streamXStream(streamXStreamProps));
//            executorService.submit(() -> tableXTable(tableXTableProps));
//            executorService.submit(() -> streamXTable(streamXTableProps));

            // Shutdown hook to clean up
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                executorService.shutdown();
                log.info("Shutting down executor service...");
            }));
        }
    }

    private static void tableXTable(Properties properties) {
        StreamsBuilder builder = new StreamsBuilder();
        KTable<String, Transaction> txnTbl = builder.table(kafkaProperties.getTxnTopic(), Consumed.with(Serdes.String(), txnSerde));
        KTable<String, Payment> payTbl = builder.table(kafkaProperties.getPayTopic(), Consumed.with(Serdes.String(), paySerde));
        txnTbl.toStream().peek((key, value) -> log.info("tableXTable: txnTbl contents: {}:{}", key, value));
        payTbl.toStream().peek((key, value) -> log.info("tableXTable: payTbl contents: {}:{}", key, value));

        KTable<String, TransactionXPayment> joinedTbl = txnTbl.join(
            payTbl,
            TransactionNPaymentConsumerDemo::joinTxnPay
//                joinWindows
//                TableJoined.with(Serdes.String(), txnSerde, paySerde)
        );
        joinedTbl.toStream().peek((key, value) -> log.info("tableXTable: joinedTbl contents: {}:{}", key, value));

        ConsumerHelper.startKafkaStreams(builder.build(), properties);
    }

    private static void streamXStream(Properties properties) {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Transaction> txnStream = builder.stream(kafkaProperties.getTxnTopic(), Consumed.with(Serdes.String(), txnSerde));
        KStream<String, Payment> payStream = builder.stream(kafkaProperties.getPayTopic(), Consumed.with(Serdes.String(), paySerde));

        // Debug prints to peek into stream. Non-terminal op.
        txnStream.peek((key, value) -> log.info("streamXStream: txnStream contents: {}", key));
        payStream.peek((key, value) -> log.info("streamXStream: payStream contents: {}:{}:{}", key, value.id(), value.status()));
        int windowSize = 20;
        int advanceSize = 5;

        // check https://kafka.apache.org/20/documentation/streams/developer-guide/dsl-api.html#streams-developer-guide-dsl-joins-co-partitioning

        TimeWindows tumblingWindow = TimeWindows.ofSizeAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0));
        TimeWindows hoppingWindow = TimeWindows.ofSizeAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0)).advanceBy(Duration.ofSeconds(advanceSize));
//        payStream.groupByKey().windowedBy(tumblingWindow).count().toStream().peek(
//            (key, value) -> log.info("streamXStream: payStream contents (tumbling count - last {}s): {}:{}", windowSize, key, value)
//        );
//        payStream.groupByKey().windowedBy(hoppingWindow).count().toStream().peek(
//            (key, value) -> log.info("streamXStream: payStream contents (hopping count - past {}s every {}s): {}:{}", windowSize, advanceSize, key, value)
//        );
        KStream<String, TransactionXPayment> joinedStream = txnStream.outerJoin(
                payStream,
                TransactionNPaymentConsumerDemo::joinTxnPay,
                JoinWindows.ofTimeDifferenceAndGrace(Duration.ofSeconds(windowSize), Duration.ofSeconds(0)),
                StreamJoined.with(Serdes.String(), txnSerde, paySerde)
        );

        // Debug prints to peek into stream. Non-terminal op.
        joinedStream.filter((key, value) -> value.isComplete()).peek((key, value) -> log.info("streamXStream: Complete Join: {}", value));
        joinedStream.filter((key, value) -> value.missingTxn()).peek((key, value) -> log.info("streamXStream: Missing txn: {}", value));
        joinedStream.filter((key, value) -> value.missingPay()).peek((key, value) -> log.info("streamXStream: Missing pay: {}", value));

        ConsumerHelper.startKafkaStreams(builder.build(), properties);
    }

    private static void streamXTable(Properties properties) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Transaction> txnStream = builder.stream(kafkaProperties.getTxnTopic(), Consumed.with(Serdes.String(), txnSerde));
        KTable<String, Payment> payTbl = builder.table(kafkaProperties.getPayTopic(), Consumed.with(Serdes.String(), paySerde));
        txnStream.peek((key, value) -> log.info("streamXTable: txnStream contents: {}:{}", key, value));
        payTbl.toStream().peek((key, value) -> log.info("streamXTable: payTbl contents: {}:{}", key, value));

//        JoinWindows joinWindows = JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5));
//        KTable<String, String> joinedTbl = txnStream.join(
//                payTbl,
//                TransactionNPaymentConsumerDemo::joinTxnPay,
//                joinWindows,
//                TableJoined.with(Serdes.String(), txnSerde, paySerde)
//        );
//        joinedTbl.toStream().peek((key, value) -> log.info("joinedTbl contents: {}:{}", key, value));

        ConsumerHelper.startKafkaStreams(builder.build(), properties);
    }

    static TransactionXPayment joinTxnPay(Transaction txn, Payment pay) {
        String transactionId = null;
        String paymentId = null;
        String customerId = null;
        String paymentStatus = null;
        if (pay != null) {
            paymentId = pay.id();
            paymentStatus = pay.status();
        } if (txn != null) {
            transactionId = txn.id();
            customerId = txn.customerId();
        }
        return new TransactionXPayment(transactionId, paymentId, customerId, paymentStatus);
    }


}
