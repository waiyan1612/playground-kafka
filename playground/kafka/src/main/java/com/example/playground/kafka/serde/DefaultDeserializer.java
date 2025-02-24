package com.example.playground.kafka.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultDeserializer<T> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeserializer.class);
    public static final String CUSTOM_VALUE_DESERIALIZER_TYPE = "value.deserializer.type";

    private final ObjectMapper objectMapper;
    private Class<T> targetType;

    public DefaultDeserializer () {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> configs, boolean isKey) {
        targetType = (Class<T>) configs.get(CUSTOM_VALUE_DESERIALIZER_TYPE);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, targetType);
        } catch (Exception e) {
            log.error("Error in deserializing bytes", e);
        }
        return null;
    }
}
