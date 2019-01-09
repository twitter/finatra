package com.twitter.finatra.kafka.utils

import com.twitter.finagle.Addr.{Bound, Failed, Neg, Pending}
import com.twitter.finagle.Address.Inet
import com.twitter.finagle.{Addr, Address, Namer}
import com.twitter.inject.Logging
import com.twitter.util.{Await, Promise, Witness}
import java.net.InetSocketAddress

object BootstrapServerUtils extends Logging {

  def lookupBootstrapServers(dest: String): String = {
    if (!dest.startsWith("/")) {
      info(s"Resolved Kafka Dest = $dest")
      dest
    } else {
      info(s"Resolving Kafka Bootstrap Servers: $dest")
      val promise = new Promise[Seq[InetSocketAddress]]()
      val resolveResult = Namer
        .resolve(dest).changes
        .register(new Witness[Addr] {
          override def notify(note: Addr): Unit = note match {
            case Pending =>
            case Bound(addresses, _) =>
              val socketAddresses = toAddresses(addresses)
              promise.setValue(socketAddresses)
            case Failed(t) =>
              promise
                .setException(new IllegalStateException(s"Unable to find addresses for $dest", t))
            case Neg =>
              promise.setException(new IllegalStateException(s"Unable to bind addresses for $dest"))
          }
        })

      val socketAddress = Await.result(promise)
      resolveResult.close()
      val servers =
        socketAddress.take(5).map(a => s"${a.getAddress.getHostAddress}:${a.getPort}").mkString(",")
      info(s"Resolved $dest = " + servers)
      servers
    }
  }

  private def toAddresses(addresses: Set[Address]): Seq[InetSocketAddress] = {
    addresses.flatMap {
      case Inet(addr, _) => Some(addr)
      case unknown =>
        warn(s"Found unknown address type looking up bootstrap servers: $unknown")
        None
    }.toSeq
  }
}
