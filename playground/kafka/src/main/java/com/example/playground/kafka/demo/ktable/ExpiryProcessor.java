package com.example.playground.kafka.demo.ktable;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class ExpiryProcessor<K, V, K2, V2> implements Processor<K, V, K2, V2> {

    private static final Logger log = LoggerFactory.getLogger(ExpiryProcessor.class);

    private final String stateStoreName;
    private final long ttl;
    private KeyValueStore<K2, Long> timestampStore;

    public ExpiryProcessor(String stateStoreName, long ttl) {
        this.stateStoreName = stateStoreName;
        this.ttl = ttl;
    }

    @Override
    public void init(ProcessorContext<K2, V2> context) {
        this.timestampStore = context.getStateStore(stateStoreName);
        context.schedule(Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            log.info("Schedule triggered, ts:{}, taskId:{}, store:{}", timestamp, context.taskId(), stateStoreName);
            try (final KeyValueIterator<K2, Long> all = timestampStore.all()) {
                while (all.hasNext()) {
                    final KeyValue<K2, Long> kv = all.next();
                    if (kv.value != null && kv.value < timestamp) {
                        log.info("Sending tombstone for key:{} since kv.value({}) < {}", kv.key, kv.value, timestamp);
                        context.forward(new Record<>(kv.key, null, 0, null));
                    }
                }
            }
        });
    }

    // Suppress because we will use the same key for K and K2
    @SuppressWarnings("unchecked")
    @Override
    public void process(Record<K, V> record) {
        // this gets invoked for each new record we consume. If it's a tombstone, delete
        // it from our state store. Otherwise, store the record timestamp.
        if (record.value() == null) {
            timestampStore.delete((K2) record.key());
            log.info("REMOVED key={} from {}", record.key(), stateStoreName);
        } else {
            // Set new records to expire after 2m. Alternatively, this can be extracted from each record.value() if it has a ttl field
            long expiresAt = Instant.now().toEpochMilli() + ttl;
            timestampStore.put((K2) record.key(), expiresAt);
            log.info("UPDATED key={} to expire from {} at {}", record.key(), stateStoreName, expiresAt);
        }
    }
}
