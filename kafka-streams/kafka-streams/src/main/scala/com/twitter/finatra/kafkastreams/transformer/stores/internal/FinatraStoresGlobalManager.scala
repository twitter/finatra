package com.twitter.finatra.kafkastreams.transformer.stores.internal

import com.twitter.finatra.kafkastreams.transformer.aggregation.TimeWindowed
import com.twitter.finatra.kafkastreams.transformer.domain.CompositeKey
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraKeyValueStore
import com.twitter.finatra.kafkastreams.transformer.stores.FinatraReadOnlyKeyValueStore
import com.twitter.finatra.kafkastreams.transformer.stores.NonLocalQueryKeyException
import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.KafkaPartitioner
import com.twitter.util.logging.Logging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.processor.TaskId
import scala.collection.JavaConverters._

/**
 * This class stores a global list of all Finatra Key Value Stores for use by Finatra's
 * queryable state functionality.
 *
 * Note: We maintain or own global list of state stores so we can directly retrieve the
 * FinatraKeyValueStore responsible for the queried key instead of having to iterate through
 * all key value stores hosted in this Kafka Streams instance.
 */
object FinatraStoresGlobalManager extends Logging {

  private val taskStoreNameToGroupId =
    new ConcurrentHashMap[String, Int](8).asScala

  private val taskIdToFinatraStore =
    new ConcurrentHashMap[StoreKey, FinatraKeyValueStore[_, _]]().asScala

  /**
   * Add a Finatra Key Value store to the global manager so that it can be queried externally
   *
   * @param processorContextStateDir Directory where this instance of Kafka Streams stores its
   *                                 state (retrievable through the processor context class)
   * @param taskId                   TaskId associated with the store being added
   * @param store                    Store being added
   * @tparam K Type of key in the store
   * @tparam V Type of value in the store
   */
  def addStore[K, V](
    processorContextStateDir: File,
    taskId: TaskId,
    store: FinatraKeyValueStore[K, V]
  ): Unit = {
    val stateDirFlag = stateDirFlagValue(processorContextStateDir)
    val storeKey = StoreKey(stateDirFlag.toString, taskId, store.name)
    info(s"Add store: $storeKey")
    taskIdToFinatraStore.put(storeKey, store)

    taskStoreNameToGroupId.put(store.name, taskId.topicGroupId)
  }

  /**
   * Remove a Finatra Key Value store so it can no longer be queried externally
   *
   * @param processorContextStateDir Directory where this instance of Kafka Streams stores its
   *                                 state (retrievable through the processor context class)
   * @param taskId                   TaskId associated with the store being added
   * @param store                    Store being added
   * @tparam K Type of key in the store
   * @tparam V Type of value in the store
   */
  def removeStore[K, V](
    processorContextStateDir: File,
    taskId: TaskId,
    store: FinatraKeyValueStore[K, V]
  ): Unit = {
    val stateDirFlag = stateDirFlagValue(processorContextStateDir)
    val storeKey = StoreKey(stateDirFlag.toString, taskId, store.name)
    info(s"Remove store: $storeKey")
    taskIdToFinatraStore.remove(storeKey)
  }

  /**
   * Get a Finatra Key Value store so that it can be queried externally
   *
   * @param stateDirFlag Directory where this instance of Kafka Streams stores its state
   *                     (retrievable through the stateDir flag specified in KafkaStreamsTwitterServer)
   * @param storeName Name of the state store
   * @param numPartitions Number of partitions associated with the state store
   * @param keyBytes Key bytes being queried used to determine which state store is responsible
   *                 for this key (since multiple state stores may be hosted in this Kafka Streams
   *                 instance)
   * @tparam K Type of key in the store
   * @tparam V Type of value in the store
   *
   * @return A FinatraReadOnlyKeyValueStore for use by queryable state endpoints
   */
  def getStore[K, V](
    stateDirFlag: File,
    storeName: String,
    numPartitions: Int,
    keyBytes: Array[Byte]
  ): FinatraReadOnlyKeyValueStore[K, V] = {
    val storeKey = lookupStoreKey(stateDirFlag, numPartitions, storeName, keyBytes)
    throwIfNoStore(
      storeKey,
      taskIdToFinatraStore
        .getOrElse(storeKey, throw new InvalidStateStoreException(s"Cannot find $storeKey"))
        .asInstanceOf[FinatraKeyValueStore[K, V]]
    )
  }

  /**
   * Get a Finatra Window store so that it can be queried externally
   *
   * @param stateDirFlag Directory where this instance of Kafka Streams stores its state
   *                     (retrievable through the stateDir flag specified in KafkaStreamsTwitterServer)
   * @param storeName Name of the state store
   * @param numPartitions Number of partitions associated with the state store
   * @param keyBytes Key bytes being queried used to determine which state store is responsible
   *                 for this key (since multiple state stores may be hosted in this Kafka Streams
   *                 instance)
   * @tparam K Type of key in the store
   * @tparam V Type of value in the store
   *
   * @return A FinatraReadOnlyKeyValueStore for use by queryable state endpoints
   */
  def getWindowedStore[K, V](
    stateDirFlag: File,
    storeName: String,
    numPartitions: Int,
    keyBytes: Array[Byte]
  ): FinatraReadOnlyKeyValueStore[TimeWindowed[K], V] = {
    val storeKey = lookupStoreKey(stateDirFlag, numPartitions, storeName, keyBytes)
    throwIfNoStore(
      storeKey,
      taskIdToFinatraStore
        .getOrElse(storeKey, throw new InvalidStateStoreException(s"Cannot find $storeKey"))
        .asInstanceOf[FinatraKeyValueStore[TimeWindowed[K], V]]
    )
  }

  /**
   * Get a Finatra Composite Window store so that it can be queried externally
   *
   * @param stateDirFlag Directory where this instance of Kafka Streams stores its state
   *                     (retrievable through the stateDir flag specified in KafkaStreamsTwitterServer)
   * @param storeName Name of the state store
   * @param numPartitions Number of partitions associated with the state store
   * @param keyBytes Key bytes being queried used to determine which state store is responsible
   *                 for this key (since multiple state stores may be hosted in this Kafka Streams
   *                 instance)
   * @tparam K Type of key in the store
   * @tparam V Type of value in the store
   *
   * @return A FinatraReadOnlyKeyValueStore for use by queryable state endpoints
   */
  def getWindowedCompositeStore[PK, SK, V](
    stateDirFlag: File,
    storeName: String,
    numPartitions: Int,
    keyBytes: Array[Byte]
  ): FinatraReadOnlyKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V] = {
    val storeKey = lookupStoreKey(stateDirFlag, numPartitions, storeName, keyBytes)
    throwIfNoStore(
      storeKey,
      taskIdToFinatraStore
        .getOrElse(storeKey, throw new InvalidStateStoreException(s"Cannot find $storeKey"))
        .asInstanceOf[FinatraKeyValueStore[TimeWindowed[CompositeKey[PK, SK]], V]]
    )
  }

  /**
   * Get the primary key bytes or throw an exception if the specified key is not local to this
   * Kafka Streams instance
   *
   * @param partitioner           KafkaPartitioner used to determine which Kafka Streams instance
   *                              is hosting a key
   * @param currentServiceShardId The shard id of this Kafka Streams instance
   * @param primaryKey            Primary key being queried
   * @param keySerializer         Serializer for the primary key being queried
   * @tparam K Type of the primary key being queried
   *
   * @return The primary key bytes
   */
  def primaryKeyBytesIfLocalKey[K](
    partitioner: KafkaPartitioner,
    currentServiceShardId: ServiceShardId,
    primaryKey: K,
    keySerializer: Serializer[K]
  ): Array[Byte] = {
    val primaryKeyBytes = keySerializer.serialize("", primaryKey)
    val partitionsToQuery = partitioner.shardIds(primaryKeyBytes)
    if (partitionsToQuery.head != currentServiceShardId) {
      throw NonLocalQueryKeyException(primaryKey, partitionsToQuery)
    }
    primaryKeyBytes
  }

  /* Private */

  private def lookupStoreKey(
    stateDir: File,
    numPartitions: Int,
    storeName: String,
    keyBytes: Array[Byte]
  ): StoreKey = {
    val partitionId = KafkaPartitioner.partitionId(numPartitions = numPartitions, keyBytes)

    val topicGroupId = taskStoreNameToGroupId
      .getOrElse(
        storeName,
        throw new InvalidStateStoreException(
          s"State store group id not found for $storeName in map " +
            s"taskStoreNameToGroupId = $taskStoreNameToGroupId and " +
            s"taskIdToFinatraStore = $taskIdToFinatraStore")
      )

    val taskId = new TaskId(topicGroupId, partitionId.id)
    debug(
      s"LookupTaskId with NumPartitions: $numPartitions store $storeName and key $keyBytes = $taskId \t $taskStoreNameToGroupId $taskIdToFinatraStore")
    StoreKey(stateDir.toString, taskId, storeName)
  }

  private def stateDirFlagValue[V, K](processorContextStateDir: File): String = {
    processorContextStateDir.toPath
      .iterator().asScala.toSeq
      .dropRight(2)
      .mkString("/", "/", "")
  }

  private def throwIfNoStore[SK, SV](storeKey: StoreKey, result: FinatraKeyValueStore[SK, SV]) = {
    if (result == null) {
      throw new InvalidStateStoreException(storeKey + " not found")
    }

    debug(s"Get store $storeKey = $result ${result.taskId}")
    result
  }

  // Note: Since FinatraStoresGlobalManager stores store in static global fields, we include
  // the stateDir so that each store key is scoped to an individual instance of Kafka Streams,
  // thus avoiding potential flakiness when running JVM tests in parallel
  private case class StoreKey(stateDir: String, taskId: TaskId, storeName: String)

}
