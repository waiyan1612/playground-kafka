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

/**
 * Ref: <a href="https://developer.confluent.io/confluent-tutorials/schedule-ktable-ttl/kstreams/">How to expire KTable rows based on TTL in Kafka Streams</a>
 */
public class KTableTtlEmitter<KIn, VIn, KOut, Long> implements Processor<KIn, VIn, KOut, Long> {

    private static final Logger log = LoggerFactory.getLogger(KTableTtlEmitter.class);

    private static final long MAX_AGE_MS = 60 * 1000L;
    private final String storeName;

    private KeyValueStore<KOut, Long> timestampStore;
    private ProcessorContext<KOut, Long> context;

    public KTableTtlEmitter(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<KOut, Long> context) {
        this.timestampStore = context.getStateStore(storeName);
        this.context = context;
        this.context.schedule(Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, this::purgeExpiredRecords);
    }

    @Override
    public void process(Record<KIn, VIn> record) {
        // this gets invoked for each new record we consume. If it's a tombstone, delete
        // it from our state store. Otherwise, store the record timestamp.
        if (record.value() == null) {
            log.info("CLEANING key={}", record.key());
            timestampStore.delete(record.key());
        } else {
            log.info("UPDATING key={}", record.key());
            timestampStore.put(record.key(), context.currentStreamTimeMs());
        }
    }

    private void purgeExpiredRecords(long currentTimeStamp) {

        // delete if record.value + MAX_AGE_MS < currentTimeStamp
        final java.lang.Long cutoff = currentTimeStamp - MAX_AGE_MS;

        try (final KeyValueIterator<KOut, Long> all = timestampStore.all()) {
            while (all.hasNext()) {
                final KeyValue<KOut, Long> record = all.next();
                if (record.value != null && record.value < cutoff) {
                    // record's last update was older than the cutoff, so emit a tombstone.
                    context.forward(new Record(record.key, null, 0, null));
                }
            }
        }
    }
}