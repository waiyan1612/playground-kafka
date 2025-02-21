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
        final String appId = "txn-pay-table-x-table";
        final String storeName = "txn-x-pay-tbl-store";

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
        txnTbl.toStream().peek((key, value) -> log.info("tableXTable: txnTbl contents: {}:{}", key, value));
        payTbl.toStream().peek((key, value) -> log.info("tableXTable: payTbl contents: {}:{}", key, value));

        KTable<String, TransactionXPayment> joinedTbl = txnTbl.outerJoin(
                payTbl,
                TransactionXPayment::fromTransactionAndPayment,
                Materialized.<String, TransactionXPayment>as(Stores.persistentKeyValueStore(storeName))
                        .withKeySerde(Serdes.String()).withValueSerde(txnXPaySerde)
        );

//        joinedTbl.toStream().process(new KTableTtlEmitter<String, String, String, String>(storeName), storeName);

        joinedTbl.toStream().peek((key, value) -> log.info("tableXTable: joinedTbl contents: {}:{}", key, value));
        Topology streamTopology =  builder.build();

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
            try (KeyValueIterator<String, TransactionXPayment> iterator = store.all()) {
                while (iterator.hasNext()) {
                    KeyValue<String, TransactionXPayment> entry = iterator.next();
                    log.info("{}:{}:{}", entry.key, entry.value.paymentId(), entry.value.paymentStatus());
                }
            } catch (Exception e) {
                log.error("Failed to read KTable: {}", e.getMessage(), e);
            }
            try {
                log.info("Finished reading KTable. Sleeping for 30s");
                Thread.sleep(30 * 1000L);
            } catch (InterruptedException e) {
                log.warn("readKTable interrupted. Shutting down...");
                Thread.currentThread().interrupt();
            }
        }
    }
}
