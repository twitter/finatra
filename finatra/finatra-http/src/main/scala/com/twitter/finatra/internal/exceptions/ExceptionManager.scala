package com.twitter.finatra.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions.{DefaultExceptionMapper, ExceptionMapper}
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils.singleTypeParam
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.typeLiteral
import scala.annotation.tailrec
import scala.collection.JavaConversions.mapAsScalaConcurrentMap

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
class ExceptionManager @Inject()(
  injector: Injector,
  defaultExceptionMapper: DefaultExceptionMapper) {

  // TODO (AF-112): Investigate using com.twitter.util.Memoize.
  private val mappers = mapAsScalaConcurrentMap(
    new ConcurrentHashMap[Type, ExceptionMapper[_]]())

  // run after constructing `mappers`
  add[Throwable](defaultExceptionMapper)

  /* Public */

  def add[T <: Throwable : Manifest](mapper: ExceptionMapper[T]) {
    set(manifest[T].runtimeClass, mapper, replace = false)
  }

  def add[T <: ExceptionMapper[_]: Manifest] {
    set[T](replace = false)
  }

  def replace[T <: Throwable : Manifest](mapper: ExceptionMapper[T]) {
    set(manifest[T].runtimeClass, mapper, replace = true)
  }

  def replace[T <: ExceptionMapper[_]: Manifest] {
    set[T](replace = true)
  }

  def toResponse(request: Request, throwable: Throwable): Response = {
    val mapper = cachedGetMapper(throwable.getClass)
    mapper.asInstanceOf[ExceptionMapper[Throwable]].toResponse(request, throwable)
  }

  /* Private */

  private def set[T <: ExceptionMapper[_]: Manifest](replace: Boolean) {
    val mapperType = typeLiteral[T].getSupertype(classOf[ExceptionMapper[_]]).getType
    val throwableType = singleTypeParam(mapperType)
    set(throwableType, injector.instance[T], replace)
  }

  private def set(throwableType: Type, mapper: ExceptionMapper[_], replace: Boolean) {
    if (!replace && mappers.contains(throwableType)) {
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
  // search for parent throwable class. This will always terminate
  // because we added an ExceptionMapper[Throwable] in our constructor
  // body.
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
