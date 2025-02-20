# playground-java

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

- [Producers](playground/kafka/src/main/java/com/example/playground/kafka/producer) are initiated as part of the spring boot application can be triggered via REST calls.
  - [Swagger UI](http://localhost:8080/swagger-ui/index.html#/)
- [Demo](playground/kafka/src/main/java/com/example/playground/kafka/demo) producers and consumers can be triggered as standalone applications.

---

## Notes 

### KTable and KStream

- **KStream**: Stateless, event-driven processing.
- **KTable**: Stateful, only the latest record will be kept for the same message key. Ordering is critical, Refreshes every 30s?

Supported Joins
- Stream x Stream
- Table x Table
- Stream x Table

### Different types of windows

| Window Type       | Window Size | Overlapping? | Use Case                                                                                                                                                                                                                                                                                                                                                     |
|-------------------|-------------|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Tumbling Join** | ✅ Fixed     | ❌ No         | Periodic results. No overlapping. For example - a daily business report for each day                                                                                                                                                                                                                                                                         |
| **Hopping Join**  | ✅ Fixed     | ✅ Yes        | Periodic results. For example - a daily business report over the last seven days; or an hourly update over the last 24h. Even if no new record are processed, you want to get a result in fixed time intervals sent downstream.                                                                                                                              |
| **Sliding Join**  | ✅ Fixed     | ✅ Yes        | Re-evaluated only if the content of the window changes, ie, each time a new record enters or leaves the window. This type of window is good for a “moving average” computation as an example. As long as no new records arrive, the result (current average) does not change and thus you don’t want get the same result sent downstream over and over again |
| **Session Join**  | ❌ Variable  | ❌ No         | Depends on data events                                                                                                                                                                                                                                                                                                                                       |

- [Sliding Windows vs. Hopping Windows](https://forum.confluent.io/t/sliding-windows-vs-hopping-windows/882)
- [Apache Kafka Beyond the Basics: Windowing](https://www.confluent.io/blog/windowing-in-kafka-streams/)

### Out-of-order records and grace period
- [Difference between increasing join window size and setting grace period](https://stackoverflow.com/a/73539852)

### Consumer groups and offsets 
- [Offsets of deleted consumer groups do not get deleted correctly](https://lists.apache.org/thread/rd3q2j3gxl31z5hhctzclqwbk0bhkc3w)

Scenario

Window is 20s. No grace period, late data will be rejected.

| Timestamp | Event                                                                                                                                                                                             | `TRANSACTION_WHERE_`<br>`FIRST_PAY_WILL_FAIL_AND_`<br>`SECOND_WILL_PASS` | `TRANSACTION_WHERE_`<br>`FIRST_PAY_WILL_MISS_THE_WINDOW`         | `TRANSACTION_WHERE_`<br>`PAYMENT_WILL_NVR_BE_MADE` |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------|
| 00:00     | Send 3 transactions.<br> 1.`TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS`<br>2.`TRANSACTION_WHERE_FIRST_PAY_WILL_MISS_THE_WINDOW`<br>3.`TRANSACTION_WHERE_PAYMENT_WILL_NVR_BE_MADE` |                                                                          |                                                                  |                                                    |
| 00:10     | Send `PAYMENT_1A` for `TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS`                                                                                                                |                                                                          |                                                                  |                                                    |
| 00:20     | Window Ends.                                                                                                                                                                                      | Expects to be joined with  <br> `PAYMENT_1A` and send to downstream      | Re-drive to a retry / DLQ topic or a Global KTable               | Re-drive to a retry / DLQ topic or a Global KTable |
| 00:30     | Send `PAYMENT_1B` for `TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS`.                                                                                                               |                                                                          |                                                                  |                                                    |
|           | Send `PAYMENT_2` for `TRANSACTION_WHERE_FIRST_PAY_WILL_MISS_THE_WINDOW`.                                                                                                                          |                                                                          |                                                                  |                                                    |
| 00:40     | Window Ends.                                                                                                                                                                                      | Expects to be joined with `PAYMENT_1B` and **update** to downstream      | Expects to be joined with `PAYMENT_2` and **send** to downstream | Stay in the retry / DLQ topic or a Global KTable   |

https://github.com/confluentinc/ksql/issues/2306#issuecomment-451126057