package com.twitter.inject.thrift

class ThriftClientExceptionSource {

  /**
   * By default the "source" of a [[com.twitter.inject.thrift.ThriftClientException]] will be the thrift
   * client label set inside of the ThriftClientException. However, you can provide your own
   * ThriftClientExceptionSource where you match the ThriftClientException to a logical name. You would
   * then provide this ThriftClientExceptionSource in a module so that it is injectable to the
   * com.twitter.finatra.thrift.ThriftClientExceptionMapper.
   *
   * For example,
   *
   * MyThriftClientExceptionSourceModule extends TwitterModule {
   *   @Singleton
   *   @Provides
   *   def providesThriftClientExceptionSource(): ThriftClientExceptionSource = {
   *     new ThriftClientExceptionSource {
   *       override def apply(e: ThriftClientException): String = {
   *         e.method.serviceName match {
   *           case "SomeServiceA" => "ATeam"
   *           case "SomeOtherServiceB" => "FooComponent"
   *           case _ => e.method.serviceName
   *         }
   *       }
   *     }
   *   }
   * }
   *
   * @param e - the [[com.twitter.inject.thrift.ThriftClientException]] to match against for a source
   * @return a String that represents the logical name of the "source" of the Exception.
   *
   * @see [[com.twitter.inject.thrift.ThriftClientException]]
   * @see [[com.twitter.inject.thrift.internal.filters.ThriftClientExceptionFilter]]
   * @see com.twitter.finatra.thrift.ThriftClientExceptionMapper
   */
  def apply(e: ThriftClientException): String = {
    e.clientLabel
  }

}
