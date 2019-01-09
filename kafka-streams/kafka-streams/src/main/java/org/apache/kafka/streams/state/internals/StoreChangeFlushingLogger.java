/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.state.internals;

// SUPPRESS CHECKSTYLE:OFF LineLength
// SUPPRESS CHECKSTYLE:OFF ModifierOrder
// SUPPRESS CHECKSTYLE:OFF OperatorWrap
// SUPPRESS CHECKSTYLE:OFF HiddenField
// SUPPRESS CHECKSTYLE:OFF NeedBraces
// SUPPRESS CHECKSTYLE:OFF NestedForDepth
// SUPPRESS CHECKSTYLE:OFF JavadocStyle
// SUPPRESS CHECKSTYLE:OFF NestedForDepth
// SUPPRESS CHECKSTYLE:OFF ConstantName

import java.util.function.BiConsumer;

import org.agrona.collections.Hashing;
import org.agrona.collections.Object2ObjectHashMap;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.internals.ProcessorStateManager;
import org.apache.kafka.streams.processor.internals.RecordCollector;
import org.apache.kafka.streams.state.StateSerdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note that the use of array-typed keys is discouraged because they result in incorrect caching behavior.
 * If you intend to work on byte arrays as key, for example, you may want to wrap them with the {@code Bytes} class,
 * i.e. use {@code RocksDBStore<Bytes, ...>} rather than {@code RocksDBStore<byte[], ...>}.
 *
 * @param <K>
 * @param <V>
 */
//See FlushingStores for motivations of this class
//Note: This class is copied from Kafka Streams StoreChangeLogger with the only changes commented with "Twitter Changed"
//      The modifications provide "flushing" functionality which flushes the latest records for a given key to the changelog
//      after every Kafka commit (which triggers the flush method below)
class StoreChangeFlushingLogger<K, V> {

    protected final StateSerdes<K, V> serialization;

    private final String topic;
    private final int partition;
    private final ProcessorContext context;
    private final RecordCollector collector;

    // Twitter Changed
    private static final Logger log = LoggerFactory.getLogger(StoreChangeFlushingLogger.class);
    private final TaskId taskId;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;
    private final Object2ObjectHashMap<K, ValueAndTimestamp<V>> newEntries = new Object2ObjectHashMap<>(100000, Hashing.DEFAULT_LOAD_FACTOR);

    StoreChangeFlushingLogger(String storeName, ProcessorContext context, StateSerdes<K, V> serialization) {
        this(storeName, context, context.taskId().partition, serialization);
    }

    private StoreChangeFlushingLogger(String storeName, ProcessorContext context, int partition, StateSerdes<K, V> serialization) {
        this.topic = ProcessorStateManager.storeChangelogTopic(context.applicationId(), storeName);
        this.context = context;
        this.partition = partition;
        this.serialization = serialization;
        this.collector = ((RecordCollector.Supplier) context).recordCollector();

        // Twitter Added
        this.taskId = context.taskId();
        this.keySerializer = serialization.keySerializer();
        this.valueSerializer = serialization.valueSerializer();
    }

    void logChange(final K key, final V value) {
        if (collector != null) {
            // Twitter Added
            newEntries.put(key, new ValueAndTimestamp<>(value, context.timestamp()));
        }
    }

    // Twitter Changed
    /*
     * logChange now saves new entries into a map, which collapses entries using the same key. When flush
     * is called, we send the latest collapsed entries to the changelog topic. By buffering entries
     * before flush is called, we avoid writing every log change to the changelog topic.
     * Pros: Less messages to and from changelog. Less broker side compaction needed. Bursts of changelog messages are better batched and compressed.
     * Cons: Changelog messages are written to the changelog topic in bursts.
     */
    void flush() {
        if (!newEntries.isEmpty()) {
            newEntries.forEach(foreachConsumer);
            log.info("Task " + taskId + " flushed " + newEntries.size() + " entries into " + topic + "." + partition);
            newEntries.clear();
        }
    }

    private final BiConsumer<K, ValueAndTimestamp<V>> foreachConsumer = new BiConsumer<K, ValueAndTimestamp<V>>() {
        @Override
        public final void accept(K key, ValueAndTimestamp<V> valueAndTimestamp) {
            // Sending null headers to changelog topics (KIP-244)
            collector.send(
                topic,
                key,
                valueAndTimestamp.value,
                null,
                partition,
                valueAndTimestamp.timestamp,
                keySerializer,
                valueSerializer);
        }
    };

    class ValueAndTimestamp<V> {
        public final V value;
        public final Long timestamp;

        ValueAndTimestamp(V value, Long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ValueAndTimestamp<?> that = (ValueAndTimestamp<?>) o;

            if (value != null ? !value.equals(that.value) : that.value != null) return false;
            return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 0;
            result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
            return result;
        }
    }
}
