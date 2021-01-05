package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags
import com.twitter.finatra.kafkastreams.flushing.FlushingAwareServer
import com.twitter.util.{Duration, Future}
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.scala.kstream.KStream
import scala.reflect.ClassTag

/**
 * Async DSL calls for Kafka Streams which allows us to map / flatMap over functions
 * that return a `Future`.
 */
trait FinatraDslFlatMapAsync extends FlushingAwareServer with FinatraTransformerFlags {
  implicit class FlatMapAsyncKeyValueStream[K1: ClassTag, V1](kstream: KStream[K1, V1]) {

    /**
     * An async version of the regular kafka streams flatMapValues DSL function.
     * Takes a function which returns a `Future[Iterable[V]]` rather than just
     * an `Iterable[V]`. Note that this will generate events with duplicate keys.
     *
     * @param commitInterval The interval between commits to Kafka
     * @param numWorkers     Max number of active Futures
     * @param func           A function from `V1` to `Future[Iterable[V2]]`
     */
    def flatMapValuesAsync[V2](
      commitInterval: Duration,
      numWorkers: Int
    )(
      func: V1 => Future[Iterable[V2]]
    ): KStream[K1, V2] =
      flatMapAsync(commitInterval, numWorkers) { (key: K1, value: V1) =>
        func(value).map { iterable =>
          iterable.map { returnValue =>
            key -> returnValue
          }
        }
      }

    /**
     * An async version of the regular kafka streams map DSL function.
     * Takes a function which returns a `Future[(K, V)]` rather than just
     * a `(K, V)`.
     *
     * @param commitInterval The interval between commits to Kafka
     * @param numWorkers     Max number of active Futures
     * @param func           A function from `(K1, V1)` to `Future[(K2, V2)]`
     */
    def mapAsync[K2, V2](
      commitInterval: Duration,
      numWorkers: Int
    )(
      func: (K1, V1) => Future[(K2, V2)]
    ): KStream[K2, V2] =
      flatMapAsync(commitInterval, numWorkers)((key: K1, value: V1) => func(key, value).map(Seq(_)))

    /**
     * An async version of the regular kafka streams mapValues DSL function.
     * Takes a function which returns a `Future[V]` rather than just
     * a `V`.
     *
     * @param commitInterval The interval between commits to Kafka
     * @param numWorkers     Max number of active Futures
     * @param func           A function from `V1` to `Future[V2]`
     */
    def mapValuesAsync[V2](
      commitInterval: Duration,
      numWorkers: Int
    )(
      func: V1 => Future[V2]
    ): KStream[K1, V2] =
      flatMapAsync(commitInterval, numWorkers) { (key: K1, value: V1) =>
        func(value).map { returnValue =>
          Seq(key -> returnValue)
        }
      }

    /**
     * An async version of the regular kafka streams flatMap DSL function.
     * Takes a function which returns a `Future[Iterable[(K, V)]]`
     * rather than just an `Iterable[(K, V)]`.
     *
     * @param commitInterval The interval between commits to Kafka
     * @param numWorkers     Max number of active Futures
     * @param func           A function from `(K1, V1)` to `Future[Iterable[(K2, V2)]]`
     */
    def flatMapAsync[K2, V2](
      commitInterval: Duration,
      numWorkers: Int
    )(
      func: (K1, V1) => Future[Iterable[(K2, V2)]]
    ): KStream[K2, V2] = {
      val supplier: () => Transformer[K1, V1, (K2, V2)] = () =>
        new FlatMapAsyncTransformer(func, statsReceiver, commitInterval, numWorkers)

      kstream.transform(supplier)
    }
  }
}
