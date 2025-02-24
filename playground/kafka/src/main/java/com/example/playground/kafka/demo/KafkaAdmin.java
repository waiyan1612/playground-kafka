package com.example.playground.kafka.demo;

import com.example.playground.kafka.config.KafkaProperties;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
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
        describeTopic("txn");
        // change retention to 10m
        alterRetention("txn", 3 * 60_000L);
        describeTopic("txn");
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

    protected static void listTopics() throws InterruptedException, ExecutionException {
        ListTopicsResult listTopicsResult = admin.listTopics();
        while (!listTopicsResult.listings().isDone()) {
            log.info("Waiting topic listing");
            Thread.sleep(5000);
        }
        listTopicsResult.listings().get().forEach(topic -> log.info("{}", topic));
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

    protected static void alterRetention(String topicName, long retentionMs) throws InterruptedException {
        Map<ConfigResource, Collection<AlterConfigOp>> updateConfigs = Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, topicName),
                List.of(new AlterConfigOp(new ConfigEntry("retention.ms", String.valueOf(retentionMs)), AlterConfigOp.OpType.SET))
        );
        AlterConfigsResult alterConfigsResult = admin.incrementalAlterConfigs(updateConfigs);
        while (!alterConfigsResult.all().isDone()) {
            log.info("Waiting altering retention");
            Thread.sleep(5000);
        }
        log.info("Completed altering retention");
    }

    protected static void describeTopic(String topicName) throws InterruptedException, ExecutionException {
        List<ConfigResource> configResources = List.of(
            new ConfigResource(ConfigResource.Type.TOPIC, topicName)
        );

        // Fetch topic configurations
        Map<ConfigResource, Config> configs = admin.describeConfigs(configResources).all().get();

        // Print retention settings
        log.info("Current configs for topic: {}:", topicName);
        configs.values().forEach(config -> config.entries().forEach(ce -> log.info("{}", ce)));
    }
}
