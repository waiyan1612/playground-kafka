package com.example.playground.kafka.demo;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerHelper {

    private static final Logger log = LoggerFactory.getLogger(ConsumerHelper.class);

    private ConsumerHelper() {

    }

    public static KafkaStreams constructStreams(Topology streamTopology, Properties properties, CountDownLatch latch) {
        KafkaStreams streams = new KafkaStreams(streamTopology, properties);
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
        return streams;
    }

    public static void startKafkaStreams(final KafkaStreams streams, CountDownLatch latch) {
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
