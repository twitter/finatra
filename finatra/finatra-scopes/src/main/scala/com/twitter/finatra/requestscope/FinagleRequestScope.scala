package com.twitter.finatra.requestscope

import com.google.inject._
import com.twitter.finatra.requestscope.FinagleRequestScope.local
import com.twitter.util.Local
import java.util.concurrent.ConcurrentHashMap
import net.codingwell.scalaguice._
import scala.collection.JavaConversions._
import scala.collection.mutable

object FinagleRequestScope {
  private val local = new Local[mutable.Map[Key[_], AnyRef]]
}

class FinagleRequestScope extends Scope {

  private[this] val cachedKeys = asScalaConcurrentMap(
    new ConcurrentHashMap[Manifest[_], Key[_]]())

  /* Public Overrides */

  override def scope[T](key: Key[T], unscoped: Provider[T]): Provider[T] = {
    new Provider[T] {
      def get: T = {
        val valuesMap = getMap(key)
        valuesMap.get(key) match {
          case None => unscoped.get()
          case Some(value) => value.asInstanceOf[T]
        }
      }
    }
  }

  /* Public */

  def enter() {
    val localValue = local()
    //TODO assert(localValue.isEmpty, "A FinagleRequestScope scoping block is already in progress")
    local.update(new mutable.HashMap[Key[_], AnyRef]())
  }

  def add[T <: AnyRef : Manifest](value: T) {
    val key = createKey[T]
    add(key, value)
  }

  def add[T <: AnyRef](key: Key[T], value: T) {
    val valuesMap = getMap(key)
    valuesMap.put(key, value)
  }

  /**
   * Remove's Typed object from Request Scope.
   * NOTE: Avoid removing objects, otherwise, they will not be available
   * in request spawned threads (e.g. future pools). Instead, you can rely
   * on exit() to cleanup the Finagle Local which will lead to eventual GC
   * of the added objects.
   *
   * @tparam T Type to remove
   */
  def remove[T: Manifest]() {
    val key = createKey[T]
    val valuesMap = getMap(key)
    valuesMap.remove(key)
  }

  def exit() {
    val localValue = local()
    assert(localValue.isDefined, "No FinagleRequestScope scoping block in progress")
    local.clear()
  }

  /* Private */

  private def createKey[T: Manifest]: Key[T] = {
    cachedKeys.getOrElseUpdate(manifest[T], {
      Key.get(typeLiteral[T])
    }).asInstanceOf[Key[T]]
  }

  private def getMap(key: Key[_]) = {
    local() getOrElse {
      val lookupType = key.getTypeLiteral
      throw new OutOfScopeException(
        "Cannot access " + lookupType + " outside of a FinagledScope.\n" +
          "Ensure that FinagleRequestScopeFilter in your filter chain and FinagleRequestScopeModule is in your module list.\n" +
          "Ensure that the filter adding " + lookupType + " is configured and in your filter chain.\n" +
          "Ensure that you're injecting Provider[" + lookupType + "] if injecting into a Singleton.\n" +
          "Ensure that you're calling provider.get every time (and not caching/storing the providers result in a class val)")
    }
  }
}