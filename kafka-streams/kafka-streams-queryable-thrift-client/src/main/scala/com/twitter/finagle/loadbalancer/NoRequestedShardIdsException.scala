package com.twitter.finagle.loadbalancer

import com.twitter.finagle.{Dtab, RequestException, SourcedException}

class NoRequestedShardIdsException(val name: String, val baseDtab: Dtab, val localDtab: Dtab)
    extends RequestException
    with SourcedException {
  def this(name: String = "unknown") = this(name, Dtab.empty, Dtab.empty)

  override def exceptionMessage: String =
    s"No requested shard ids set in the local context. The ShardIdAwareRoundRobinBalancer should " +
      s"only be used when there's always a requested shard id"

  serviceName = name
}
