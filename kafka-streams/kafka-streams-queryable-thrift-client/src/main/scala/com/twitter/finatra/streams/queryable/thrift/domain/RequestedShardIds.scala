package com.twitter.finatra.streams.queryable.thrift.domain

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.context.Contexts.local

object RequestedShardIds {
  val requestedShardIdsKey: local.Key[RequestedShardIds] =
    new Contexts.local.Key[RequestedShardIds]()
}

/**
 * Requested shard ids in order of preference
 */
case class RequestedShardIds(shardIds: IndexedSeq[ServiceShardId]) {

  private[this] var nextShardIdx: Int = 0

  //TODO: Also check for Node with state = Open?
  def chooseShardId(availableShardIds: Set[ServiceShardId]): Option[ServiceShardId] = {
    var choiceNum = 0
    while (choiceNum < shardIds.size) {
      val nextShardId = shardIds(nextShardIdx)
      nextShardIdx = math.abs((nextShardIdx + 1) % shardIds.size)
      if (availableShardIds.contains(nextShardId)) {
        return Some(nextShardId)
      }
      choiceNum += 1
    }
    None
  }
}
