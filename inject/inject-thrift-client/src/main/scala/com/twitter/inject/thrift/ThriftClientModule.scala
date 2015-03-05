package com.twitter.inject.thrift

import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule
import com.twitter.inject.thrift.internal.ThriftMuxStackClientModule
import javax.inject.Singleton
import scala.reflect.ClassTag

abstract class ThriftClientModule[T: ClassTag]
  extends TwitterModule {

  override val modules = Seq(ThriftMuxStackClientModule)

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
   *
   * Note: Both server and client must have mux enabled otherwise
   * a non-descript ChannelClosedException will be seen.
   */
  val mux: Boolean = true

  @Singleton
  @Provides
  def providesClient(muxClient: ThriftMux.Client): T = {
    val labelAndDest = s"$label=$dest"

    if (mux) {
      muxClient.
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
