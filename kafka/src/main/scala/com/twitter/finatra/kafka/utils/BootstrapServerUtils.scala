package com.twitter.finatra.kafka.utils

import com.twitter.finagle.Addr.Bound
import com.twitter.finagle.Addr.Failed
import com.twitter.finagle.Addr.Neg
import com.twitter.finagle.Addr.Pending
import com.twitter.finagle.Address.Inet
import com.twitter.finagle.Addr
import com.twitter.finagle.Address
import com.twitter.finagle.Namer
import com.twitter.util.Await
import com.twitter.util.Duration
import com.twitter.util.Promise
import com.twitter.util.Witness
import com.twitter.util.logging.Logging
import java.net.InetSocketAddress

object BootstrapServerUtils extends Logging {

  /**
   * Translates the dest path into a list of servers that can be used to initialize a Kafka
   * producer or consumer. It uses [[com.twitter.util.Duration.Top]] as the timeout, effectively
   * waiting infinitely for the name resolution.
   * @param dest The path to translate.
   * @return A comma separated list of server addresses.
   */
  def lookupBootstrapServers(dest: String): String = lookupBootstrapServers(dest, Duration.Top)

  /**
   * Translates the dest path into a list of servers that can be used to initialize a Kafka
   * producer or consumer using the specified timeout.
   * @param dest The path to translate.
   * @param timeout The maximum timeout for the name resolution.
   * @return A comma separated list of server addresses.
   */
  def lookupBootstrapServers(dest: String, timeout: Duration): String = {
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

      val socketAddress = Await.result(promise.ensure(resolveResult.close()), timeout)
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
