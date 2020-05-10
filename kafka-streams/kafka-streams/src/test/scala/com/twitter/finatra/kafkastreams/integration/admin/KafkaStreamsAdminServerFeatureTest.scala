package com.twitter.finatra.kafkastreams.integration.admin

import com.twitter.finatra.kafka.serde.UnKeyedSerde
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Await
import java.nio.charset.{Charset, StandardCharsets}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}

class KafkaStreamsAdminServerFeatureTest extends KafkaStreamsFeatureTest {
  private val ApplicationId = "no-op"
  private val Source = "source"
  private val Sink = "sink"
  kafkaTopic(keySerde = UnKeyedSerde, valSerde = Serdes.String, name = Source)
  kafkaTopic(keySerde = UnKeyedSerde, valSerde = Serdes.String, name = Sink)

  override val server = new EmbeddedTwitterServer(
    new KafkaStreamsTwitterServer {
      override val name = ApplicationId
      override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
        builder.asScala
          .stream(Source)(Consumed.`with`(UnKeyedSerde, Serdes.String))
          .to(Sink)(Produced.`with`(UnKeyedSerde, Serdes.String))
      }
    },
    flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> ApplicationId),
    disableTestLogging = true
  )

  override def beforeEach(): Unit = {
    server.start()
  }

  test("admin kafka streams properties") {
    val bufBytes = getAdminResponseBytes("/admin/kafka/streams/properties")
    val result = new String(bufBytes, StandardCharsets.UTF_8)
    result.contains("application.id=no-op") should equal(true)
  }

  test("admin kafka streams topology") {
    val bufBytes = getAdminResponseBytes("/admin/kafka/streams/topology")
    val result = new String(bufBytes, Charset.forName("UTF-8"))
    result.trim() should equal("""<pre>
        |Topologies:
        |   Sub-topology: 0
        |    Source: KSTREAM-SOURCE-0000000000 (topics: [source])
        |      --> KSTREAM-SINK-0000000001
        |    Sink: KSTREAM-SINK-0000000001 (topic: sink)
        |      <-- KSTREAM-SOURCE-0000000000
        |</pre>""".stripMargin)
  }

  private def getAdminResponseBytes(path: String): Array[Byte] = {
    val response = server.httpGetAdmin(path)
    val read = Await.result(response.reader.read())
    read.isDefined should equal(true)
    val buf = read.get
    val bufBytes: Array[Byte] = new Array[Byte](buf.length)
    buf.write(bufBytes, off = 0)
    bufBytes
  }
}
