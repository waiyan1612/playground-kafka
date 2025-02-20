package com.example.playground.kafka.demo;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class ConsumerHelper {

    private static final Logger log = LoggerFactory.getLogger(ConsumerHelper.class);

    private ConsumerHelper() {

    }

    protected static void deleteConsumerGroupId(Properties properties) {
        try {
            KafkaAdmin.deleteConsumerGroupId(properties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        } catch (InterruptedException e) {
            log.error("Failed to delete consumer group id: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    protected static void resetConsumerOffsets(Properties properties, String... topics) {
        Arrays.stream(topics).forEach(
            topic -> {
                try {
                    KafkaAdmin.setConsumerOffsetLatest(properties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG), topic);
                } catch (ExecutionException | InterruptedException e) {
                    log.error("Failed to reset consumer offset: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        );
    }

    protected static void startKafkaStreams(Topology streamTopology, Properties properties) {
        try (KafkaStreams streams = new KafkaStreams(streamTopology, properties)) {
            final int retryCount = 3;
            final CountDownLatch latch = new CountDownLatch(retryCount);
            streams.setUncaughtExceptionHandler(exception -> {
                log.error(exception.getMessage(), exception);
                return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
            });
            streams.setStateListener((newState, oldState) -> {
                if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
                    log.warn("Streams transitioned from {} to {}. Counting down latch.", oldState, newState);
                    latch.countDown();
                }
            });

            streams.start();
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
            try {
                latch.await();
                log.warn("Latch over");
            } catch (final InterruptedException e) {
                log.warn("Application interrupted. Shutting down...");
                Thread.currentThread().interrupt();
            }
        }
    }
}
