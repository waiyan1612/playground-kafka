# playground-kafka

A playground to quickly test kafka producers and consumers.

## Quick Start

1. Start kafka container.
   ```shell
   docker compose -f docker/kafka.yaml up
   ```
2. Test connectivity using console producer and consumer.
   ```shell
   docker container exec -it kafka-standalone /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic random
   docker container exec -it kafka-standalone /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic random
   ```

Other commonly used commands.
```shell
# Update gradlelock file
./gradlew kafka:dependencies --write-locks
```

---

## Demo producers and consumers 

[Producers](playground/kafka/src/main/java/com/example/playground/kafka/producer) are initiated as part of the spring boot application can be triggered via REST calls.
  - [Swagger UI](http://localhost:8080/swagger-ui/index.html#/)

Demo producers and consumers can be triggered as standalone applications.
- [KTable Demo](playground/kafka/src/main/java/com/example/playground/kafka/demo/ktable/README.md)
- [KStream Demo](playground/kafka/src/main/java/com/example/playground/kafka/demo/kstream/README.md)

---
