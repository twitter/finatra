.. _kafka-streams_testing:

Testing
=======

Finatra Kafka Streams includes tooling that simplifies the process of writing highly testable services. See `TopologyFeatureTest <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/test/scala/com/twitter/finatra/streams/tests/TopologyFeatureTest.scala>`__, which includes a `FinatraTopologyTester <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/test/scala/com/twitter/finatra/streams/tests/FinatraTopologyTester.scala>`__ that integrates Kafka Streams' `TopologyTestDriver <https://kafka.apache.org/21/javadoc/org/apache/kafka/streams/TopologyTestDriver.html>`__ with a `KafkaStreamsTwitterServer <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/KafkaStreamsTwitterServer.scala>`__.