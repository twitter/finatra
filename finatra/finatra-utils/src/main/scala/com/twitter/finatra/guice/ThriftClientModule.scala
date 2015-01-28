package com.twitter.finatra.guice

import com.google.inject.Provides
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.{Thrift, ThriftMux}
import javax.inject.Singleton
import scala.reflect.ClassTag

abstract class ThriftClientModule[T: ClassTag]
  extends GuiceModule {

  /**
   * Name of client for use in metrics
   */
  val label: String

  /**
   * Destination of client (usually a wily path)
   */
  val dest: String

  /**
   * ClientId to identify client calling the thrift service.
   * Note: clientId is a def so that it's value can come from a flag
   */
  def clientId: String = ""

  /**
   * Enable thrift mux for this connection.
   * Note: Both server and client have mux enabled otherwise
   * a non-descript ChannelClosedException will be seen
   */
  val mux: Boolean = false

  @Singleton
  @Provides
  def providesClient: T = {
    val labelAndDest = s"$label=$dest"
    if (mux) {
      ThriftMux.client.
        withClientId(ClientId(clientId)).
        newIface[T](labelAndDest)
    }
    else {
      Thrift.client.
        withClientId(ClientId(clientId)).
        newIface[T](labelAndDest)
    }
  }
}
