package com.twitter.inject.thrift

import com.github.nscala_time.time
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.factory.TimeoutFactory
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import javax.inject.Singleton
import scala.reflect.ClassTag


abstract class ThriftClientModule[T: ClassTag]
  extends TwitterModule
  with time.Implicits {

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

  def requestTimeout: Duration = Duration.Top

  def connectTimeout: Duration = Duration.Top

  @Singleton
  @Provides
  def providesClient: T = {
    val labelAndDest = s"$label=$dest"

    if (mux) {
      ThriftMux.client.
        withParams(params).
        withClientId(ClientId(clientId)).
        newIface[T](labelAndDest)
    }
    else {
      Thrift.client.
        withParams(params).
        withClientId(ClientId(clientId)).
        newIface[T](labelAndDest)
    }
  }

  def params = {
    Stack.Params.empty +
      TimeoutFilter.Param(requestTimeout) +
      TimeoutFactory.Param(connectTimeout)
  }
}