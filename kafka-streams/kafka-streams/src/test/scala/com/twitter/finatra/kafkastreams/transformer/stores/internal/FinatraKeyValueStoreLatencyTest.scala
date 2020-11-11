package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.inject.{InMemoryStatsReceiverUtility, Test}
import com.twitter.finatra.kafkastreams.test.KafkaTestUtil
import org.apache.kafka.common.metrics.Metrics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.LogContext
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.internals.MockStreamsMetrics
import org.apache.kafka.streams.state.internals.{RocksDBStoreFactory, ThreadCache}
import org.apache.kafka.test.{InternalMockProcessorContext, TestUtils}
import scala.collection.JavaConverters._

class FinatraKeyValueStoreLatencyTest extends Test {

  private val context = new InternalMockProcessorContext(
    TestUtils.tempDirectory,
    Serdes.String,
    Serdes.String,
    KafkaTestUtil.createNoopRecord(),
    new ThreadCache(new LogContext("testCache"), 0, new MockStreamsMetrics(new Metrics()))
  )

  private val statsReceiver = new InMemoryStatsReceiver()
  private val statsUtil = new InMemoryStatsReceiverUtility(statsReceiver)
  private val rocksDbStore = RocksDBStoreFactory.create("FinatraKeyValueStoreTest", "TestMetrics")
  private val keyValueStore = new MetricsFinatraKeyValueStore[Int, String](
    new FinatraKeyValueStoreImpl(rocksDbStore, rocksDbStore, ScalaSerdes.Int, Serdes.String),
    statsReceiver = statsReceiver)

  private val Keys1to10 = 1 to 10
  private val Values1to10 = 'a' to 'j'
  private val KeyValues1to10 = (Keys1to10 zip Values1to10)
    .map(keyValue => new KeyValue(keyValue._1, keyValue._2.toString))
  private val Key1 = KeyValues1to10.head.key
  private val Value1 = KeyValues1to10.head.value

  private val AllLatencyStats = Seq(
    MetricsFinatraKeyValueStore.InitLatencyStatName,
    MetricsFinatraKeyValueStore.CloseLatencyStatName,
    MetricsFinatraKeyValueStore.PutLatencyStatName,
    MetricsFinatraKeyValueStore.PutIfAbsentLatencyStatName,
    MetricsFinatraKeyValueStore.PutAllLatencyStatName,
    MetricsFinatraKeyValueStore.DeleteLatencyStatName,
    MetricsFinatraKeyValueStore.FlushLatencyStatName,
    MetricsFinatraKeyValueStore.PersistentLatencyStatName,
    MetricsFinatraKeyValueStore.IsOpenLatencyStatName,
    MetricsFinatraKeyValueStore.GetLatencyStatName,
    MetricsFinatraKeyValueStore.RangeLatencyStatName,
    MetricsFinatraKeyValueStore.AllLatencyStatName,
    MetricsFinatraKeyValueStore.ApproximateNumEntriesLatencyStatName,
    MetricsFinatraKeyValueStore.DeleteRangeLatencyStatName,
    MetricsFinatraKeyValueStore.DeleteRangeExperimentalLatencyStatName,
    MetricsFinatraKeyValueStore.DeleteWithoutGettingPriorValueLatencyStatName,
    MetricsFinatraKeyValueStore.FinatraRangeLatencyStatName
  )

  private def getLatencyStat(name: String): Seq[Float] = {
    val latencyStatNamePrefix = "stores/FinatraKeyValueStoreTest"
    val latencyStatNameSuffix = "latency_us"
    statsUtil.stats(s"$latencyStatNamePrefix/$name/$latencyStatNameSuffix")
  }

  private def assertNonzeroLatency(name: String) = {
    val latencyStat = getLatencyStat(name)
    assert(latencyStat.nonEmpty, s"$name stat is empty")
    assert(latencyStat.forall(_ >= 0), s"$name call had zero latency")
  }

  private def assertAllNonzeroLatency() = {
    AllLatencyStats.map { name =>
      assertNonzeroLatency(name)
    }
  }

  override def afterEach(): Unit = {
    statsReceiver.clear()
  }

  test("Series of store operations") {
    keyValueStore.init(context, rocksDbStore)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.InitLatencyStatName)

    assert(keyValueStore.isOpen())
    assertNonzeroLatency(MetricsFinatraKeyValueStore.IsOpenLatencyStatName)

    assert(keyValueStore.persistent())
    assertNonzeroLatency(MetricsFinatraKeyValueStore.PersistentLatencyStatName)

    keyValueStore.put(Key1, Value1)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.PutLatencyStatName)

    assert(keyValueStore.get(Key1) == Value1)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.GetLatencyStatName)

    keyValueStore.putIfAbsent(Key1, Value1)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.PutIfAbsentLatencyStatName)

    keyValueStore.delete(Key1)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.DeleteLatencyStatName)

    keyValueStore.putAll(KeyValues1to10.asJava)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.PutAllLatencyStatName)

    keyValueStore.range(1, 5).close()
    assertNonzeroLatency(MetricsFinatraKeyValueStore.RangeLatencyStatName)

    keyValueStore.all().close()
    assertNonzeroLatency(MetricsFinatraKeyValueStore.AllLatencyStatName)

    keyValueStore.approximateNumEntries()
    assertNonzeroLatency(MetricsFinatraKeyValueStore.ApproximateNumEntriesLatencyStatName)

    keyValueStore.deleteRange(1, 2)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.DeleteRangeLatencyStatName)

    keyValueStore.deleteRangeExperimentalWithNoChangelogUpdates(
      Array.emptyByteArray,
      Array.emptyByteArray)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.DeleteRangeExperimentalLatencyStatName)

    keyValueStore.deleteWithoutGettingPriorValue(10)
    assertNonzeroLatency(MetricsFinatraKeyValueStore.DeleteWithoutGettingPriorValueLatencyStatName)

    keyValueStore.range(Array())
    assertNonzeroLatency(MetricsFinatraKeyValueStore.FinatraRangeLatencyStatName)

    keyValueStore.flush()
    assertNonzeroLatency(MetricsFinatraKeyValueStore.FlushLatencyStatName)

    keyValueStore.close()
    assertNonzeroLatency(MetricsFinatraKeyValueStore.CloseLatencyStatName)

    assertAllNonzeroLatency()
  }
}
