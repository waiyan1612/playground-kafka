package com.example.playground.kafka.demo.ktable;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.demo.ConsumerHelper;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.model.Transaction;
import com.example.playground.kafka.model.TransactionXPayment;
import com.example.playground.kafka.serde.CustomJsonDeserializer;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class KTableDemo {

    private static final Logger log = LoggerFactory.getLogger(KTableDemo.class);

    private static final KafkaProperties kafkaProperties = new KafkaProperties();

    private static final Serde<Transaction> txnSerde = Serdes.serdeFrom(new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Transaction.class));

    private static final Serde<Payment> paySerde = Serdes.serdeFrom(new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(Payment.class)
    );

    private static final Serde<TransactionXPayment> txnXPaySerde = Serdes.serdeFrom(new CustomJsonSerializer<>(),
            new CustomJsonDeserializer<>(TransactionXPayment.class)
    );

    public static void main(String[] args) {

        // Adding a random suffix to the kafka consumer group name to ensure a fresh set of consumer offset.
        final boolean addRandomSuffix = true;

        // This will add processors that will peek and print to console in the topology.
        final boolean debug = true;

        // This will add expiry processors that will send tombstones to txn and pay topics after x minutes.
        final boolean expireTxn = false;
        final boolean expirePay = false;

        final String appId = "txn-pay-table-x-table";
        final String storeName = "txn-x-pay-tbl-store";
        final String txnExpiryStoreName = "txn-tbl-expiry-store";
        final String payExpiryStoreName = "pay-tbl-expiry-store";

        String optSuffix = addRandomSuffix ? "-" + UUID.randomUUID() : "";
        log.info("{} will be appended to the application.id", optSuffix);
        Properties tableXTableProps = new Properties();
        tableXTableProps.put(StreamsConfig.APPLICATION_ID_CONFIG, appId + optSuffix);
        tableXTableProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());

        // Construct stream topology. There is no concept of window in table joins.
        // The table will be updated whenever there is new data from either side of the join.
        // Materialized is optional. It allows us to specify a state store that can be queried later.
        StreamsBuilder builder = new StreamsBuilder();
        KTable<String, Transaction> txnTbl = builder.table(kafkaProperties.getTxnTopic(), Consumed.with(Serdes.String(), txnSerde));
        KTable<String, Payment> payTbl = builder.table(kafkaProperties.getPayTopic(), Consumed.with(Serdes.String(), paySerde));
        if (debug) {
            txnTbl.toStream().peek((key, value) -> log.info("tableXTable: txnTbl contents: {}:{}", key, value));
            payTbl.toStream().peek((key, value) -> log.info("tableXTable: payTbl contents: {}:{}", key, value));
        }

        KTable<String, TransactionXPayment> joinedTbl = txnTbl.outerJoin(
                payTbl,
                TransactionXPayment::fromTransactionAndPayment,
                Materialized.<String, TransactionXPayment>as(Stores.persistentKeyValueStore(storeName))
                        .withKeySerde(Serdes.String()).withValueSerde(txnXPaySerde)
        );

        // If we want to use foreign key join, it is supported for left and inner joins, but not for the outer join
        // And the left should have the details to join. In the example below, we have swapped the sides for txn and pay.
        // https://www.confluent.io/blog/data-enrichment-with-kafka-streams-foreign-key-joins/
        KTable<String, TransactionXPayment> foreignKeyJoinedTbl = payTbl.leftJoin(
                txnTbl,
                Payment::transactionId,
                (pay, txn) -> TransactionXPayment.fromTransactionAndPayment(txn, pay),
                Materialized.<String, TransactionXPayment>as(Stores.persistentKeyValueStore(storeName))
                        .withKeySerde(Serdes.String()).withValueSerde(txnXPaySerde)
        );

        if (expireTxn) {
            ProcessorSupplier<String, Transaction, String, Transaction> processorSupplier = new ExpiryProcessorSupplier<>(txnExpiryStoreName, 3 * 60_000L);
            txnTbl.toStream().process(processorSupplier).to(kafkaProperties.getTxnTopic(), Produced.with(Serdes.String(), txnSerde));
        }
        if (expirePay) {
            ProcessorSupplier<String, Payment, String, Payment> processorSupplier = new ExpiryProcessorSupplier<>(payExpiryStoreName, 2 * 60_000L);
            payTbl.toStream().process(processorSupplier).to(kafkaProperties.getPayTopic(), Produced.with(Serdes.String(), paySerde));
        }
        if (debug) {
            joinedTbl.toStream().peek((key, value) -> log.info("tableXTable: joinedTbl contents: {}:{}", key, value));
            joinedTbl.toStream().to("ktable-demo");
            joinedTbl.toStream().filter((key, value) -> value != null).to("ktable-demo-no-tombstones");
        }

        Topology streamTopology =  builder.build();

        // Can use open-source tools like
        // https://zz85.github.io/kafka-streams-viz/
        log.info("Topology\n========\n{}", streamTopology.describe());

        // Build streams from the topology
        final CountDownLatch latch = new CountDownLatch(3);
        try (KafkaStreams streams = ConsumerHelper.constructStreams(streamTopology, tableXTableProps, latch)) {
            try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
                log.info("Starting threads in the background ...");
                // This is to start the kafka stream and keep it running.
                executorService.submit(() -> ConsumerHelper.startKafkaStreams(streams, latch));
                // This is to check local store.
                executorService.submit(() -> readTableFromStore(streams, storeName));
                // Shutdown hook to clean up
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    executorService.shutdown();
                    log.info("Shutting down executor service...");
                }));
            }
        }
    }

    /***
     * Example to read from the persisted local store
     */
    protected static void readTableFromStore(final KafkaStreams streams, String storeName) {
        ReadOnlyKeyValueStore<String, TransactionXPayment> store =
                streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()));
        while(true) {
            log.info("Interactive Query: Started reading from ReadOnlyKeyValueStore...");
            try (KeyValueIterator<String, TransactionXPayment> iterator = store.all()) {
                while (iterator.hasNext()) {
                    KeyValue<String, TransactionXPayment> entry = iterator.next();
                    log.info("Interactive Query: {}:{}:{}", entry.key, entry.value.paymentId(), entry.value.paymentStatus());
                }
            } catch (Exception e) {
                log.error("Interactive Query: Failed to read from ReadOnlyKeyValueStore: {}", e.getMessage(), e);
            }
            log.info("Interactive Query: Finished reading from ReadOnlyKeyValueStore. Sleeping for 5s");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                log.warn("readKTable interrupted. Shutting down...");
                Thread.currentThread().interrupt();
            }
        }
    }
}
