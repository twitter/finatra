package com.twitter.inject.thrift

import com.google.inject.util.Providers
import com.twitter.finagle._
import com.twitter.finagle.service.{RetryPolicy, RetryingFilter, TimeoutFilter}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.TwitterPrivateModule
import com.twitter.inject.thrift.ThriftClientModule.ThriftClientFilter
import com.twitter.inject.thrift.internal._
import com.twitter.scrooge._
import com.twitter.util._
import javax.inject.Provider
import org.joda.time.Duration
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect._

class ThriftClientFilters(
  module: TwitterPrivateModule,
  statsReceiver: StatsReceiver)
  extends RetryUtils {

  private[inject] val filters: ArrayBuffer[Provider[ThriftClientFilter]] = ArrayBuffer[Provider[ThriftClientFilter]]()

  private type MethodName = String
  private[inject] val methodFilters =
    mutable.Map[MethodName, Seq[Provider[ThriftClientFilter]]]()
      .withDefaultValue(Vector())

  /* Public */

  def filtersAdded = {
    filters.nonEmpty || methodFilters.nonEmpty
  }

  def createThriftClientFilterChain = {
    ThriftClientFilterChain(
      filters.toSeq,
      methodFilters.toMap)
  }

  def filter(filter: ThriftClientFilter) = {
    filters += Providers.of(filter)
    this
  }

  def filter[T <: ThriftClientFilter : Manifest] = {
    filters += module.getProvider[T].asInstanceOf[Provider[ThriftClientFilter]]
    this
  }

  /*
   * TODO: Perform runtime type-check similar to methodResultToFailureFilter.
   * One way to do this would be to add an additional type param, but this complicates the API...
   * e.g. assertMethodResultType[ThriftClientMethodFilterTypeParam](methodResult.metaData, methodName)
   */
  def filterMethod[T <: ThriftClientMethodFilter[_] : Manifest](
    methodResult: ThriftStructCodec3[_]) = {

    val newFilter = module.getProvider[T].asInstanceOf[Provider[ThriftClientFilter]]
    addMethodFilter(methodResult, newFilter)
  }

  def filterMethod(
    methodResult: ThriftStructCodec3[_],
    filter: ThriftClientMethodFilter[_]) = {

    val newFilter = Providers.of(filter).asInstanceOf[Provider[ThriftClientFilter]]
    addMethodFilter(methodResult, newFilter)
  }

  /**
   * Converts a successful future into a Failure exception by matching methodResult's successful result using the isFailure predicate
   *
   * @param isFailure Predicate to determine if successful future results are actually failures
   * @param msg Message for use in com.twitter.finagle.Failure
   * @param flags Flags for use in com.twitter.finagle.Failure
   */
  def methodResultToFailureFilter[T: Manifest](
    methodResult: ThriftStructCodec3[_],
    isFailure: T => Boolean,
    msg: => String = "",
    flags: => Long = Failure.Restartable) = {

    val metadata = methodResult.metaData
    val methodName = getMethodName(metadata)
    assertMethodResultType(metadata, methodName)

    filterMethod(
      methodResult,
      new MethodResultToFailureFilter[T](isFailure, msg, flags))

    this
  }

  /* Utils for common filters */

  def retryingFilter(policy: RetryPolicy[Try[Nothing]]) = {
    filter(
      new RetryingFilter[FinatraThriftClientRequest, Any](
        policy,
        DefaultTimer.twitter,
        statsReceiver))
  }

  def exponentialRetryingFilter(
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[Try[Nothing], Boolean]) = {

    retryingFilter(
      exponentialRetry(
        start = start,
        multiplier = multiplier,
        numRetries = numRetries,
        shouldRetry = shouldRetry))
  }

  def globalTimeoutFilter(globalTimeout: Duration) = {
    val twitterTimeout = globalTimeout.toTwitterDuration

    filter(
      new TimeoutFilter[FinatraThriftClientRequest, Any](
        twitterTimeout,
        new GlobalRequestTimeoutException(twitterTimeout),
        DefaultTimer.twitter))
  }

  private def assertMethodResultType[T: Manifest](metadata: ThriftStructMetaData[_], methodName: String): Unit = {
    val typeParamManifest = manifest[T]

    val methodResultTypeManifest = metadata.fields find { thriftStruct: ThriftStructField[_] =>
      thriftStruct.`name` == "success"
    } flatMap { thriftStruct: ThriftStructField[_] =>
      thriftStruct.manifest
    } getOrElse {
      throw new scala.RuntimeException("success return type not found")
    }

    assert(
      typeParamManifest == methodResultTypeManifest,
      s"ThriftClientModule error: 'isFailure' type mismatch for method '$methodName'. $typeParamManifest does not match expected type of $methodResultTypeManifest")
  }

  private val MethodNameExtractor = """.*\$(.*)\$.*""".r

  private def getMethodName(metadata: ThriftStructMetaData[_]): String = {
    val MethodNameExtractor(methodName) = metadata.structName
    methodName
  }

  private def addMethodFilter(
    methodResult: ThriftStructCodec3[_],
    newFilter: Provider[ThriftClientFilter]): ThriftClientFilters = {

    val methodName = getMethodName(methodResult.metaData)
    val existingFilters = methodFilters(methodName)

    val newFilters = existingFilters :+ newFilter

    methodFilters += methodName -> newFilters
    this
  }
}
