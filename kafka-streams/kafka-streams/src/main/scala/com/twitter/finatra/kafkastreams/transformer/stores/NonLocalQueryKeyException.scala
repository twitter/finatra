package com.twitter.finatra.kafkastreams.transformer.stores

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId

case class NonLocalQueryKeyException[K](
  primaryKey: K,
  serviceShardIds: IndexedSeq[ServiceShardId])
    extends Exception {

  override def getMessage: String = {
    s"Non local key $primaryKey. Query $serviceShardIds which are responsible for this key. Note: " +
      s"the first shard id in this list is always responsible for this key, and any remaining ids" +
      s"will only be hosting this key if standby replicas are enabled"
  }
}
