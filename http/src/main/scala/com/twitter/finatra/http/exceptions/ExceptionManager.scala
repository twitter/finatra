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
 * A class to register ExceptionMappers and handle exceptions.
 *
 * Given some exception, an ExceptionManager will find an ExceptionMapper
 * to handle that particular class of exceptions. If the mapper for that
 * exception isn't registered, ExceptionManager will try its parent
 * class, and so on, until it reaches the Throwable class. At that point
 * the DefaultExceptionMapper will run which should be defined over all
 * Throwables.
 *
 * @throws java.lang.IllegalStateException when an exception type is
 * registered twice.
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
class ExceptionManager(
  injector: Injector,
  defaultExceptionMapper: DefaultExceptionMapper,
  statsReceiver: StatsReceiver) {

  private val mappers = new ConcurrentHashMap[Type, ExceptionMapper[_]]().asScala

  /* Public */

  def add[T <: Throwable : Manifest](mapper: ExceptionMapper[T]) {
    add(manifest[T].runtimeClass, mapper)
  }

  def add[T <: ExceptionMapper[_]: Manifest] {
    val mapperType = typeLiteral[T].getSupertype(classOf[ExceptionMapper[_]]).getType
    val throwableType = singleTypeParam(mapperType)
    add(throwableType, injector.instance[T])
  }

  def toResponse(request: Request, throwable: Throwable): Response = {
    val mapper = cachedGetMapper(throwable.getClass)
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

  private def add(throwableType: Type, mapper: ExceptionMapper[_]) {
    if (mappers.contains(throwableType)) {
      throw new IllegalStateException(s"ExceptionMapper for $throwableType already registered")
    } else {
      mappers(throwableType) = mapper // mutation
    }
  }

  // Assumes mappers are never explicitly registered after configuration
  // phase, otherwise we'd need to invalidate the cache.
  private def cachedGetMapper(cls: Class[_]): ExceptionMapper[_] = {
    mappers.getOrElseUpdate(cls, getMapper(cls))
  }

  // Get mapper for this throwable class if it exists, otherwise
  // search for parent throwable class. If we reach the Throwable
  // class then return the default mapper.
  //
  // Note: we avoid getOrElse so we have tail recursion
  @tailrec
  private def getMapper(cls: Class[_]): ExceptionMapper[_] = {
    if (cls == classOf[Throwable]) {
      defaultExceptionMapper
    } else {
      mappers.get(cls) match {
        case Some(mapper) => mapper
        case None => getMapper(cls.getSuperclass)
      }
    }
  }
}
