package com.example.playground.kafka.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultSerializer<T> implements Serializer<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultSerializer.class);
    private final ObjectMapper objectMapper;

    public DefaultSerializer() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            return objectMapper.writeValueAsString(data).getBytes();
        } catch (Exception e) {
            log.error("Error in serializing object {}", data, e);
        }
        return new byte[0];
    }
}
