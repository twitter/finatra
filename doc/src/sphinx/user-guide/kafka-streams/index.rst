.. _kafka-streams:

Finatra Kafka Streams
=====================

Finatra has native integration with `Kafka Streams <https://kafka.apache.org/documentation/streams>`__ to easily build Kafka Streams applications on top of a `TwitterServer <https://github.com/twitter/twitter-server>`__.

Features
--------

-  Intuitive `DSL <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/internal/utils/FinatraDslV2Implicits.scala>`__ for topology creation, compatible with the `Kafka Streams DSL <https://kafka.apache.org/21/documentation/streams/developer-guide/dsl-api.html>`__
-  Full Kafka Streams metric integration, exposed as `TwitterServer Metrics <https://twitter.github.io/twitter-server/Features.html#metrics>`__
-  `RocksDB integration <#rocksdb>`__
-  `Queryable State <#queryable-state>`__
-  `Rich testing functionality <testing.html>`__

Basics
------

With `KafkaStreamsTwitterServer <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/KafkaStreamsTwitterServer.scala>`__,
a fully functional service can be written by simply configuring the Kafka Streams Builder via the ``configureKafkaStreams()`` lifecycle method. See the `examples <examples.html>`__ section.

Transformers
~~~~~~~~~~~~
Implement custom `transformers <https://kafka.apache.org/21/javadoc/org/apache/kafka/streams/kstream/Transformer.html>`__ using `FinatraTransformerV2 <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/streams/transformer/FinatraTransformerV2.scala>`__.

Aggregations
^^^^^^^^^^^^
There are several included aggregating transformers, which may be used when configuring a ``StreamsBuilder``
  + ``sample``
  +  ``sum``
  +  ``compositeSum``
[TODO : Add as available]
-  Unique counting with HyperLogLog
-  TopK counting

Stores
------
-  Stores
-  Timers / TimerStores

RocksDB
~~~~~~~
In addition to using `state stores <https://kafka.apache.org/21/javadoc/org/apache/kafka/streams/state/Stores.html>`__, you may also use a RocksDB-backed store. This affords all of the advantages of using `RocksDB <https://rocksdb.org/>`__, including efficient range scans.

Queryable State
~~~~~~~~~~~~~~~
Finatra Kafka Streams supports directly querying state from a store. This can be useful for creating a service that serves data aggregated within a local Topology. You can use `static partitioning <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams-static-partitioning/src/main/scala/com/twitter/finatra/streams/partitioning/StaticPartitioning.scala>`__ to query an instance deterministically known to hold a key.

See how queryable state is used in the following `example <examples.html#queryable-state>`__.

Queryable Stores
^^^^^^^^^^^^^^^^
  -  `QueryableFinatraKeyValueStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/streams/query/QueryableFinatraKeyValueStore.scala>`__
  -  `QueryableFinatraWindowStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/streams/query/QueryableFinatraWindowStore.scala>`__
  -  `QueryableFinatraCompositeWindowStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/streams/query/QueryableFinatraCompositeWindowStore.scala>`__
