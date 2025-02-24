package com.example.playground.kafka.producer;

import com.example.playground.kafka.config.KafkaProperties;
import com.example.playground.kafka.model.Payment;
import com.example.playground.kafka.serde.CustomJsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
public class PaymentProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProducer.class);


    private final Producer<String, Payment> producer;
    private final String paymentTopic;

    @Autowired
    public PaymentProducer(KafkaProperties kafkaProperties) {
        paymentTopic = kafkaProperties.getPayTopic();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getServers());
        properties.put("client.id", "payment-producer@playground");
        properties.put("key.serializer", StringSerializer.class);
        properties.put("value.serializer", CustomJsonSerializer.class);
        producer = new KafkaProducer<>(properties);
    }

    public void send(List<Payment> payments) {
        payments.forEach(this::send);
    }

    private void send(Payment payment) {
        try {
            // NOTE: MESSAGE KEY is transactionId, not paymentId. This is because for stream x stream joins,
            // we need the records we want to join to go into the same partition.
            // If we use paymentId as the key here, later we will need to repartition and stream
            // to a tmp / pass-through topic for the joins to read from.
            ProducerRecord<String, Payment> producerRecord = new ProducerRecord<>(paymentTopic, payment.transactionId(), payment);
            RecordMetadata metadata = producer.send(producerRecord).get();
            log.info("Record {} sent to partition {} with offset {}",
                    payment, metadata.partition(), metadata.offset());
        } catch (ExecutionException e) {
            log.error("Error in sending record: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Interrupted :{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
