package com.example.playground.kafka.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;

@Component
public class KafkaProperties {

    private final String servers;

    private final String txnTopic;

    private final String payTopic;

    private final String customerTopic;

    public KafkaProperties() {
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("kafka.yaml"));
        Properties yamlProperties = Objects.requireNonNull(yamlFactory.getObject());

        servers = yamlProperties.getProperty("bootstrap.servers");
        txnTopic = yamlProperties.getProperty("topics.transaction.name");
        payTopic = yamlProperties.getProperty("topics.payment.name");
        customerTopic = yamlProperties.getProperty("topics.customer.name");
    }

    public String getServers() {
        return servers;
    }

    public String getTxnTopic() {
        return txnTopic;
    }

    public String getPayTopic() {
        return payTopic;
    }

    public String getCustomerTopic() {
        return customerTopic;
    }
}
