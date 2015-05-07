package com.twitter.inject.thrift.internal

import com.twitter.inject.Logging
import com.twitter.inject.thrift.internal.ThreadsafeFutureMethodInvocation.proceedDirectly
import com.twitter.util.Future
import org.aopalliance.intercept.MethodInvocation

object FinatraThriftClientRequest {

  def create(label: String, invocation: MethodInvocation): FinatraThriftClientRequest = {
    val method = invocation.getMethod
    FinatraThriftClientRequest(
      invocation = invocation,
      label = label,
      methodName = method.getName)
  }
}

/**
 * A generic thrift request representing a request to an arbitrary thrift method.
 *
 * Since Scrooge generated thrift clients do not contain common request types or a filter chain,
 * we intercept each thrift method using AOP, and extract common request information in this class.
 *
 * @param label Label naming the thrift client
 * @param invocation An AOP Alliance MethodInvocation capturing the method and arguments of a thrift method call
 * @param methodName Name of thrift method being called
 */
case class FinatraThriftClientRequest(
  label: String,
  invocation: MethodInvocation,
  methodName: String)
  extends Logging {

  /* Public */

  /*
   * Note: Normally we'd call invocation.proceed() however proceed() does not work correctly
   * if called multiple times from different threads. This is problematic since we commonly
   * use a RetryFilter, which uses a "Finagle Default Timer" thread that differs from
   * the incoming Netty thread. As such, we call our own thread-safe proceed which is ok
   * since we don't expect additional method interceptors to be wrapping thrift client methods.
   */
  def executeThriftMethod(): Future[Any] = {
    proceedDirectly(invocation)
  }
}