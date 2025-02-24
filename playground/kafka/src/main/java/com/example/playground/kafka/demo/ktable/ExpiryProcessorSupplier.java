package com.example.playground.kafka.demo.ktable;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Collections;
import java.util.Set;

public class ExpiryProcessorSupplier<K, V, K2, V2> implements ProcessorSupplier<K, V, K2, V2> {

    private final String stateStoreName;
    private final long ttl;

    public ExpiryProcessorSupplier(String stateStoreName, long ttl) {
        this.stateStoreName = stateStoreName;
        this.ttl = ttl;
    }

    @Override
    public Processor<K, V, K2, V2> get() {
        return new ExpiryProcessor<>(stateStoreName, ttl);
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        return Collections.singleton(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(stateStoreName),
                Serdes.String(),
                Serdes.Long()
        ));
    }
}
