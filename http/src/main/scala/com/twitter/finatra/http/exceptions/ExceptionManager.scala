package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.utils.ClassUtils
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils.singleTypeParam
import com.twitter.inject.exceptions.DetailedNonRetryableSourcedException
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import net.codingwell.scalaguice.typeLiteral
import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
 * A class to register [[com.twitter.finatra.http.exceptions.ExceptionMapper]]s
 * and handle exceptions.
 *
 * Given an exception, the ExceptionManager will find an
 * [[com.twitter.finatra.http.exceptions.ExceptionMapper]] to handle
 * that particular class of exceptions. If the mapper for that exception
 * isn't registered, the ExceptionManager will try the exception's parent class,
 * and so on, until it reaches the Throwable class. The framework registers a "root"
 * exception mapper over Throwable which will eventually be invoked. Users are
 * free to register their own ExceptionMapper[Throwable] which overrides the
 * "root" exception mapper.
 *
 * @see [[com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper]]
 *
 * Note: When searching for the parent exception mapper, it would be nice
 * to traverse the entire class linearization so it works for
 * traits/mixins too [1]. Unfortunately, implementing this would require
 * a lot more reflection and it might not be threadsafe [2]. Doing it in
 * Scala 2.11 might be easier and safer.
 *
 * [1] http://stackoverflow.com/questions/15623498/handy-ways-to-show-linearization-of-a-class
 * [2] http://docs.scala-lang.org/overviews/reflection/thread-safety.html
 */
@Singleton
class ExceptionManager  (
  injector: Injector,
  statsReceiver: StatsReceiver) {

  private val mappers = new ConcurrentHashMap[Type, ExceptionMapper[_]]().asScala

  /* Public */

  /**
   * Add a [[com.twitter.finatra.http.exceptions.ExceptionMapper]] over type [[T]] to the manager.
   * If a mapper has already been added for the given [[T]], it will be replaced.
   *
   * @param mapper - [[com.twitter.finatra.http.exceptions.ExceptionMapper]] to add
   * @tparam T - exception class type which should subclass [[java.lang.Throwable]]
   */
  def add[T <: Throwable : Manifest](mapper: ExceptionMapper[T]): Unit = {
    register(manifest[T].runtimeClass, mapper)
  }

  /**
   * Add a collection of [[com.twitter.finatra.http.exceptions.ExceptionMapper]] as defined by a
   * [[com.twitter.finatra.http.exceptions.ExceptionMapperCollection]]. The collection is iterated
   * over and each mapper contained therein is added. If a mapper has already been added for the given
   * type T in ExceptionMapper[T], it will be replaced.
   *
   * @param collection - [[com.twitter.finatra.http.exceptions.ExceptionMapperCollection]] to add.
   */
  def add(collection: ExceptionMapperCollection): Unit = {
    collection.foreach(add(_))
  }

  /**
   * Add a [[com.twitter.finatra.http.exceptions.ExceptionMapper]] by type [[T]]
   *
   * @tparam T - ExceptionMapper type T which should subclass [[com.twitter.finatra.http.exceptions.ExceptionMapper]]
   */
  def add[T <: ExceptionMapper[_]: Manifest]: Unit = {
    val mapperType = typeLiteral[T].getSupertype(classOf[ExceptionMapper[_]]).getType
    val throwableType = singleTypeParam(mapperType)
    register(throwableType, injector.instance[T])
  }

  /**
   * Returns a [[com.twitter.finagle.http.Response]] as computed by the matching
   * [[com.twitter.finatra.http.exceptions.ExceptionMapper]] to the given throwable.
   *
   * @param request - a [[com.twitter.finagle.http.Request]]
   * @param throwable - [[java.lang.Throwable]] to match against registered ExceptionMappers.
   * @return a [[com.twitter.finagle.http.Response]]
   */
  def toResponse(request: Request, throwable: Throwable): Response = {
    val mapper = getMapper(throwable.getClass)
    val response = mapper.asInstanceOf[ExceptionMapper[Throwable]].toResponse(request, throwable)
    RouteInfo(request).foreach { info =>
      statException(info, request, throwable, response)
    }
    response
  }

  /* Private */

  private def statException(
    routeInfo: RouteInfo,
    request: Request,
    throwable: Throwable,
    response: Response): Unit = {
    statsReceiver.counter(
      "route",
      routeInfo.sanitizedPath,
      request.method.toString,
      "status",
      response.status.code.toString,
      "mapped",
      exceptionDetails(throwable))
      .incr()
  }

  private def exceptionDetails(throwable: Throwable): String = {
    val className = ClassUtils.simpleName(throwable.getClass)
    throwable match {
      case sourceDetails: DetailedNonRetryableSourcedException => className + "/" + sourceDetails.toDetailsString
      case _ => className
    }
  }

  // Last entry by type overrides any previous entry.
  private def register(throwableType: Type, mapper: ExceptionMapper[_]): Unit = {
    mappers.update(throwableType, mapper)
  }

  // Get mapper for this throwable class if it exists, otherwise
  // search for parent throwable class. If we reach the Throwable
  // class then return the default mapper.
  //
  // Note: we avoid getOrElse so we have tail recursion
  @tailrec
  private def getMapper(cls: Class[_]): ExceptionMapper[_] = {
    mappers.get(cls) match {
      case Some(mapper) => mapper
      case None => getMapper(cls.getSuperclass)
    }
  }
}
