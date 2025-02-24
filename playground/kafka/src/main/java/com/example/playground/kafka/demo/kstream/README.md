# KStream Demo

1. Run SpringBoot [Application](../../Application.java).
2. Run [KStreamDemo](KStreamDemo.java). There will be some warnings / errors because the topics have not been created yet.
    - A few flags can be configured here. The following steps assume these configs are used.
      ```java
        // This will add processors that will peek and print to console in the topology.
        final boolean debug = true;
      ```

## Readings

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
