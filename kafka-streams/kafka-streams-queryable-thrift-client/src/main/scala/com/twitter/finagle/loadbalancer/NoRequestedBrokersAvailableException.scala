package com.twitter.finagle.loadbalancer

import com.twitter.finagle.{Dtab, RequestException, SourcedException}

class NoRequestedBrokersAvailableException(
  val name: String,
  val baseDtab: Dtab,
  val localDtab: Dtab)
    extends RequestException
    with SourcedException {
  def this(name: String = "unknown") = this(name, Dtab.empty, Dtab.empty)

  override def exceptionMessage: String =
    s"No requested hosts are available for $name, Dtab.base=[${baseDtab.show}], Dtab.local=[${localDtab.show}]"

  serviceName = name
}
