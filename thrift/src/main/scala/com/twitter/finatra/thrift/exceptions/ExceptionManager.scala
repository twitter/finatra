package com.twitter.finatra.thrift.exceptions

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils.singleTypeParam
import com.twitter.util.Future
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import net.codingwell.scalaguice.typeLiteral
import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
 * A class to register [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]]s and
 * handle exceptions
 *
 * Given an exception, the ExceptionManager will find an
 * [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] to handle that particular
 * class of exceptions. If the mapper for that exception isn't registered, the ExceptionManager
 * will try the exception's parent class, until it reaches a Throwable class. If no Throwable class
 * exception mapper is found, it won't handle the exception. Users are free to register their own
 * ExceptionMapper[Throwable] which will be the root exception mapper.
 */
@Singleton
class ExceptionManager(injector: Injector, statsReceiver: StatsReceiver) {

  private val mappers = new ConcurrentHashMap[Type, ExceptionMapper[_, _]]().asScala

  /* Public */

  /**
   * Add a [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] by type [[T]]
   *
   * @tparam T - ExceptionMapper type T, which should be a subclass of
   * [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]]
   */
  def add[T <: ExceptionMapper[_, _]: Manifest]: Unit = {
    val mapperType = typeLiteral[T].getSupertype(classOf[ExceptionMapper[_, _]]).getType
    val throwableType = singleTypeParam(mapperType)
    register(throwableType, injector.instance[T])
  }

  /**
   * Add a [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over type [[T]] to the manager.
   * If a mapper has already been added for the given [[T]], it will be replaced.
   *
   * @param mapper - [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] to add
   * @tparam T - exception class type which should be a subclass of [[java.lang.Throwable]]
   */
  def add[T <: Throwable: Manifest](mapper: ExceptionMapper[T, _]): Unit = {
    register(manifest[T].runtimeClass, mapper)
  }

  /**
   * Returns a Future[Rep] as computed by the matching
   * [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] to the given throwable.
   *
   * @param throwable - [[java.lang.Throwable]] to match against registered ExceptionMappers.
   * @return a response wrapped in Future
   */
  def handleException[Rep](throwable: Throwable): Future[Rep] = {
    val mapper = getMapper(throwable.getClass)
    mapper.asInstanceOf[ExceptionMapper[Throwable, Rep]].handleException(throwable)
  }

  /* Private */

  // Last entry by type overrides any previous entry.
  private def register(throwableType: Type, mapper: ExceptionMapper[_, _]): Unit = {
    mappers.update(throwableType, mapper)
  }

  // Get mapper for this throwable class if it exists, otherwise
  // search for parent Throwable class. If we reach the Throwable
  // class then return the default mapper.
  //
  // Note: we avoid getOrElse so we have tail recursion
  @tailrec
  private def getMapper(cls: Class[_]): ExceptionMapper[_, _] = {
    mappers.get(cls) match {
      case Some(mapper) => mapper
      case None => getMapper(cls.getSuperclass)
    }
  }
}
