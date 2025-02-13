package com.example.playground.kafka.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomJsonSerializer<T> implements Serializer<T> {

    private static final Logger log = LoggerFactory.getLogger(CustomJsonSerializer.class);
    private final ObjectMapper objectMapper;

    public CustomJsonSerializer() {
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
