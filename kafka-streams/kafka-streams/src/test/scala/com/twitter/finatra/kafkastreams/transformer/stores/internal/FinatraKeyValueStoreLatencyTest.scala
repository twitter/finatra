package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.kafka.test.utils.InMemoryStatsUtil
import com.twitter.inject.Test
import org.apache.kafka.common.metrics.Metrics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.LogContext
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.processor.internals.MockStreamsMetrics
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.internals.ThreadCache
import org.apache.kafka.test.{InternalMockProcessorContext, NoOpRecordCollector, TestUtils}
import scala.collection.JavaConversions._

class FinatraKeyValueStoreLatencyTest extends Test {

  private var context: InternalMockProcessorContext = _

  private val statsReceiver = new InMemoryStatsReceiver()
  private val statsUtil = new InMemoryStatsUtil(statsReceiver)
  private val keyValueStore = new FinatraKeyValueStoreImpl[Int, String](
    name = "FinatraKeyValueStoreTest",
    statsReceiver = statsReceiver
  )

  private val Keys1to10 = 1 to 10
  private val Values1to10 = 'a' to 'j'
  private val KeyValues1to10 = (Keys1to10 zip Values1to10)
    .map(keyValue => new KeyValue(keyValue._1, keyValue._2.toString))
  private val Key1 = KeyValues1to10.head.key
  private val Value1 = KeyValues1to10.head.value

  // TODO: add `FinatraKeyValueStoreImpl.DeleteRangeExperimentalLatencyStatName` for testing
  private val AllLatencyStats = Seq(
    FinatraKeyValueStoreImpl.InitLatencyStatName,
    FinatraKeyValueStoreImpl.CloseLatencyStatName,
    FinatraKeyValueStoreImpl.PutLatencyStatName,
    FinatraKeyValueStoreImpl.PutIfAbsentLatencyStatName,
    FinatraKeyValueStoreImpl.PutAllLatencyStatName,
    FinatraKeyValueStoreImpl.DeleteLatencyStatName,
    FinatraKeyValueStoreImpl.FlushLatencyStatName,
    FinatraKeyValueStoreImpl.PersistentLatencyStatName,
    FinatraKeyValueStoreImpl.IsOpenLatencyStatName,
    FinatraKeyValueStoreImpl.GetLatencyStatName,
    FinatraKeyValueStoreImpl.RangeLatencyStatName,
    FinatraKeyValueStoreImpl.AllLatencyStatName,
    FinatraKeyValueStoreImpl.ApproximateNumEntriesLatencyStatName,
    FinatraKeyValueStoreImpl.DeleteRangeLatencyStatName,
    FinatraKeyValueStoreImpl.DeleteWithoutGettingPriorValueLatencyStatName,
    FinatraKeyValueStoreImpl.FinatraRangeLatencyStatName
  )

  private def getLatencyStat(name: String): Seq[Float] = {
    val latencyStatNamePrefix = "stores/FinatraKeyValueStoreTest"
    val latencyStatNameSuffix = "latency_us"
    statsUtil.getStat(s"$latencyStatNamePrefix/$name/$latencyStatNameSuffix")
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

  override def beforeEach(): Unit = {
    context = new InternalMockProcessorContext(
      TestUtils.tempDirectory,
      Serdes.Integer,
      Serdes.String,
      new NoOpRecordCollector,
      new ThreadCache(new LogContext(), 0, new MockStreamsMetrics(new Metrics()))
    ) {
      override def getStateStore(name: String): StateStore = {
        val storeBuilder = Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(name),
            Serdes.Integer(),
            Serdes.String()
          )

        val store = storeBuilder.build
        store.init(this, store)
        store
      }
    }
  }

  override def afterEach(): Unit = {
    statsReceiver.clear()
  }

  test("Series of store operations") { // TODO: test deleteRangeExperimental()
    keyValueStore.init(context, null)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.InitLatencyStatName)

    assert(keyValueStore.isOpen())
    assertNonzeroLatency(FinatraKeyValueStoreImpl.IsOpenLatencyStatName)

    assert(keyValueStore.persistent())
    assertNonzeroLatency(FinatraKeyValueStoreImpl.PersistentLatencyStatName)

    keyValueStore.put(Key1, Value1)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.PutLatencyStatName)

    assert(keyValueStore.get(Key1) == Value1)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.GetLatencyStatName)

    keyValueStore.putIfAbsent(Key1, Value1)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.PutIfAbsentLatencyStatName)

    keyValueStore.delete(Key1)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.DeleteLatencyStatName)

    keyValueStore.putAll(KeyValues1to10)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.PutAllLatencyStatName)

    keyValueStore.range(1, 5).close()
    assertNonzeroLatency(FinatraKeyValueStoreImpl.RangeLatencyStatName)

    keyValueStore.all().close()
    assertNonzeroLatency(FinatraKeyValueStoreImpl.AllLatencyStatName)

    keyValueStore.approximateNumEntries()
    assertNonzeroLatency(FinatraKeyValueStoreImpl.ApproximateNumEntriesLatencyStatName)

    keyValueStore.deleteRange(1, 2)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.DeleteRangeLatencyStatName)

    keyValueStore.deleteWithoutGettingPriorValue(10)
    assertNonzeroLatency(FinatraKeyValueStoreImpl.DeleteWithoutGettingPriorValueLatencyStatName)

    keyValueStore.range(Array())
    assertNonzeroLatency(FinatraKeyValueStoreImpl.FinatraRangeLatencyStatName)

    keyValueStore.flush()
    assertNonzeroLatency(FinatraKeyValueStoreImpl.FlushLatencyStatName)

    keyValueStore.close()
    assertNonzeroLatency(FinatraKeyValueStoreImpl.CloseLatencyStatName)

    assertAllNonzeroLatency()
  }
}
