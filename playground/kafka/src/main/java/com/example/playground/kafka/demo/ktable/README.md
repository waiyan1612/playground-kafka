# KTable Demo

1. Run SpringBoot [Application](../../Application.java).
2. Run [KTableDemo](KTableDemo.java). There will be some warnings / errors because the topics have not been created yet.
   - A few flags can be configured here. The following steps assume these configs are used.
     ```java
       // This will add processors that will peek and print to console in the topology.
       final boolean debug = true;

       // This will add expiry processors that will send tombstones to txn and pay topics after x minutes.
       final boolean expireTxn = false;
       final boolean expirePay = false;
     ``` 
3. Produce Txn and Pay records using [TxnXPayProducer](../txnxpay/TxnXPayProducer.java).
4. We should start to see the records in `KTableDemo` console.
   > - `TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS` should show the latest `PAYMENT_1` - which is `ACCEPTED`
   > - `TRANSACTION_WHERE_FIRST_PAY_WILL_MISS_THE_WINDOW` should show `PAYMENT_2`
   > - `TRANSACTION_WHERE_PAYMENT_WILL_NVR_BE_MADE` should show `null` for the payment since there is no payment related data for this.

### When is KTable updated?
- KTables update state whenever a new record arrives, but writes to the changelog topic happen in batches, by default every 30 seconds (`commit.interval.ms`).
- If caching is enabled, the KTable buffers updates in-memory and writes in batches (flushing on commit interval or when cache is full).
- If KTable is piping back to a topic (via a stream), new records are written to the topic for each update happening on KTable (new records, update on certain fields, tombstones for left / right table). 

### When are entries removed from KTable?

| When                                                      | Removed? | Notes                                                                                                                                                                 |
|-----------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| After consumer is restarted                               | ❌ No     | We are persisting using RocksDB.                                                                                                                                      |
| When the messages in parent topics `txn` and `pay` expire | ❌ No     | [KafkaAdmin](../KafkaAdmin.java) has a function to alter the retention. We can test that KTable still retains the join topics after the original messags are expired. |
| When tombstone messages are sent to `txn` and `pay`       | ✅ Yes    | See below.                                                                                                                                                            |

Continue from step 4, execute the below steps.   

5. Run [PayTombstoneProducer](../pay/PayTombstoneProducer.java).
   > Observe that `TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS` no longer shows `PAYMENT_1` information.

6. Run [TxnTombstoneProducer](../txn/TxnTombstoneProducer.java).
   > Observe that `TRANSACTION_WHERE_FIRST_PAY_WILL_FAIL_AND_SECOND_WILL_PASS` is no longer present in the KTable since both sides of the join no longer have the data.

#### ExpiryProcessor
It's possible to expire records in KTable based on TTL. [ExpiryProcessor.java](ExpiryProcessor.java) is a quick example on how this can be achieved. We can run the demo with `expireTxn` and `expirePay` flags set to true and observe something similar to step 5-6 above.   

## Readings

- [Medium blog - Kafka Streams — Optimizing RocksDB](https://verticalserve.medium.com/kafka-streams-optimizing-rocksdb-99a6cc14bc93)
- [Medium blog - Kafka-Streams and rocksdb in the space-time continuum and a little bit of configuration](https://blog.dy.engineering/kafka-streams-and-rocksdb-in-the-space-time-continuum-and-a-little-bit-of-configuration-40edb5ee9ed7)
- [Confluent - When do KTable records expire if you don’t tombstone them](https://forum.confluent.io/t/when-do-ktable-records-expire-if-you-dont-tombstone-them/6967)
- [How to expire KTable rows based on TTL in Kafka Streams](https://developer.confluent.io/confluent-tutorials/schedule-ktable-ttl/kstreams/).
- [Kafka Streams State Ttl Patterns](https://github.com/thriving-dev/kafka-streams-state-ttl-patterns)
