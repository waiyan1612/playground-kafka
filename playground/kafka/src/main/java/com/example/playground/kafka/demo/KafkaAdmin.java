package com.example.playground.kafka.demo;

import com.example.playground.kafka.config.KafkaProperties;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Helper class for kafka administrative actions such as topic and offset management
 */
public class KafkaAdmin {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdmin.class);

    private static final KafkaProperties kafkaProperties = new KafkaProperties();
    private static final AdminClient admin;

    static {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getServers());
        admin = AdminClient.create(config);
    }

    public static void main(String [] args) throws InterruptedException, ExecutionException {
//        createTopics();
        deleteTopics();
    }

    protected static void deleteConsumerGroupId(String consumerGroupId) throws InterruptedException {
        DeleteConsumerGroupsResult deleteTopicsResult = admin.deleteConsumerGroups(List.of(consumerGroupId));
        while (!deleteTopicsResult.all().isDone()) {
            log.info("Pending deletion");
            Thread.sleep(5000);
        }
        log.info("Completed deletion");
    }

    protected static void setConsumerOffsetLatest(String consumerGroupId, String topic) throws ExecutionException, InterruptedException {
        log.info("Displaying current offsets for group {}: {}",
                consumerGroupId, admin.listConsumerGroupOffsets(consumerGroupId).all().get().get(consumerGroupId));

        TopicDescription topicDescription = admin.describeTopics(Collections.singletonList(topic)).allTopicNames().get().get(topic);
        Map<TopicPartition, OffsetSpec> requestLatestOffsetSpecs = topicDescription.partitions().stream().collect(
                Collectors.toMap(
                        x -> new TopicPartition(topic, x.partition()),
                        x -> OffsetSpec.latest()
                )
        );

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = admin.listOffsets(requestLatestOffsetSpecs).all().get();
        log.info("Fetched latest offsets for {} topic: {}", topic, latestOffsets);

        Map<TopicPartition, OffsetAndMetadata> newOffsets = latestOffsets.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        topicPartition -> new OffsetAndMetadata(topicPartition.getValue().offset())
                )
        );
        admin.alterConsumerGroupOffsets(consumerGroupId, newOffsets).all().get();

        log.info("Offsets successfully reset to latest for consumer group: {}", consumerGroupId);
        log.info("Displaying current offsets for group {}: {}",
                consumerGroupId, admin.listConsumerGroupOffsets(consumerGroupId).all().get().get(consumerGroupId));
    }

    protected static void createTopics() throws InterruptedException {
        CreateTopicsResult createTopicsResult = admin.createTopics(List.of(
                new NewTopic(kafkaProperties.getTxnTopic(), Optional.empty(), Optional.empty()),
                new NewTopic(kafkaProperties.getTxnTopic(), Optional.empty(), Optional.empty())
        ));
        while (!createTopicsResult.all().isDone()) {
            log.info("Waiting topic creation");
            Thread.sleep(5000);
        }
        log.info("Completed creation");
    }

    protected static void deleteTopics() throws InterruptedException {
        DeleteTopicsResult deleteTopicsResult = admin.deleteTopics(List.of(
                kafkaProperties.getTxnTopic(),
                kafkaProperties.getPayTopic()
        ));
        while (!deleteTopicsResult.all().isDone()) {
            log.info("Pending deletion");
            Thread.sleep(5000);
        }
        log.info("Completed deletion");
    }
}
