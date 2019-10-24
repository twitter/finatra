package com.twitter.inject.server

trait Ports extends com.twitter.server.TwitterServer {

  /**
   * Returns the bound port representing the external HTTP interface of this server.
   *
   * @return a defined `Some(Int)` representing the port of the external HTTP interface
   *         if defined and bound, None otherwise.
   */
  def httpExternalPort: Option[Int] = None

  /**
   * Returns the bound port representing the external HTTPS interface of this server.
   *
   * @return a defined `Some(Int)` representing the port of the external HTTPS interface
   *         if defined and bound, None otherwise.
   */
  def httpsExternalPort: Option[Int] = None

  /**
   * Returns the bound port representing the external Thrift interface of this server.
   *
   * @return a defined `Some(Int)` representing the port of the external Thrift interface
   *         if defined and bound, None otherwise.
   */
  def thriftPort: Option[Int] = None
}
