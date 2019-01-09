.. _kafka-streams_examples:

Examples
========

The `integration tests <https://github.com/twitter/finatra/blob/develop/kafka-streams/kafka-streams/src/test/scala/com/twitter/unittests/integration>`__ serve as a good collection of example Finatra Kafka Streams servers.

Word Count Server
-----------------

We can build a lightweight server which counts the unique words from an input topic, storing the results in RocksDB.

.. code:: scala

    class WordCountRocksDbServer extends KafkaStreamsTwitterServer {

      override val name = "wordcount"
      private val countStoreName = "CountsStore"

      override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
        builder.asScala
          .stream[Bytes, String]("TextLinesTopic")(Consumed.`with`(Serdes.Bytes, Serdes.String))
          .flatMapValues(_.split(' '))
          .groupBy((_, word) => word)(Serialized.`with`(Serdes.String, Serdes.String))
          .count()(Materialized.as(countStoreName))
          .toStream
          .to("WordsWithCountsTopic")(Produced.`with`(Serdes.String, ScalaSerdes.Long))
      }
    }

Queryable State
~~~~~~~~~~~~~~~

We can then expose a Thrift endpoint enabling clients to directly query the state via `interactive queries <https://kafka.apache.org/21/documentation/streams/developer-guide/interactive-queries.html>`__.

.. code:: scala

    class WordCountRocksDbServer extends KafkaStreamsTwitterServer with QueryableState {

      ...

      final override def configureThrift(router: ThriftRouter): Unit = {
        router
          .add(
            new WordCountQueryService(
              queryableFinatraKeyValueStore[String, Long](
                storeName = countStoreName,
                primaryKeySerde = Serdes.String
              )
            )
          )
      }
    }

In this example, ``WordCountQueryService`` is an underlying Thrift service.