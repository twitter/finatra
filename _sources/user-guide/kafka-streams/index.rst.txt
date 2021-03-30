.. _kafka-streams:

Finatra Kafka Streams
=====================

Finatra has native integration with `Kafka Streams <https://kafka.apache.org/documentation/streams>`__ to easily build Kafka Streams applications on top of a `TwitterServer <https://github.com/twitter/twitter-server>`__.

.. note::

    Versions of finatra-kafka and finatra-kafka-streams that are published against Scala 2.12 use Kafka 2.2, versions of that are published against Scala 2.13 use Kafka 2.5. This simplified cross-version support is ephemeral until we can drop Kafka 2.2. 

Features
--------

-  Intuitive `DSL <https://github.com/twitter/finatra/tree/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/dsl>`__ for topology creation, compatible with the `Kafka Streams DSL <https://kafka.apache.org/21/documentation/streams/developer-guide/dsl-api.html>`__
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

Implement custom `transformers <https://kafka.apache.org/21/javadoc/org/apache/kafka/streams/kstream/Transformer.html>`__ using `FinatraTransformer <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/transformer/FinatraTransformer.scala>`__.

Aggregations
^^^^^^^^^^^^

There are several included aggregating transformers, which may be used when configuring a ``StreamsBuilder``
  + ``aggregate``
  + ``sample``
  + ``sum``

Stores
------

RocksDB
~~~~~~~

In addition to using `state stores <https://kafka.apache.org/21/javadoc/org/apache/kafka/streams/state/Stores.html>`__, you may also use a RocksDB-backed store. This affords all of the advantages of using `RocksDB <https://rocksdb.org/>`__, including efficient range scans.

Queryable State
~~~~~~~~~~~~~~~

Finatra Kafka Streams supports directly querying state from a store. This can be useful for creating a service that serves data aggregated within a local Topology. You can use `static partitioning <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams-static-partitioning/src/main/scala/com/twitter/finatra/kafkastreams/partitioning/StaticPartitioning.scala>`__ to query an instance deterministically known to hold a key.

See how queryable state is used in the following `example <examples.html#queryable-state>`__.

Queryable Stores
^^^^^^^^^^^^^^^^

  -  `QueryableFinatraKeyValueStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/query/QueryableFinatraKeyValueStore.scala>`__
  -  `QueryableFinatraWindowStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/query/QueryableFinatraWindowStore.scala>`__
  -  `QueryableFinatraCompositeWindowStore <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/main/scala/com/twitter/finatra/kafkastreams/query/QueryableFinatraCompositeWindowStore.scala>`__
