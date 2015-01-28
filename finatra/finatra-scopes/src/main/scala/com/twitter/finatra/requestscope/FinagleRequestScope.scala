package com.twitter.finatra.requestscope

import com.google.inject._
import com.google.inject.internal.CircularDependencyProxy
import com.twitter.util.Local
import java.util.{HashMap => JHashMap, Map => JMap}
import net.codingwell.scalaguice._


/**
 * A Guice 'Request Scope' implemented with com.twitter.util.Local
 *
 * @see https://github.com/google/guice/wiki/CustomScopes
 */
class FinagleRequestScope extends Scope {

  private[this] val local = new Local[JMap[Key[_], AnyRef]]
  private val scopeName = "FinagleRequestScope"

  /* Public Overrides */

  override def scope[T](key: Key[T], unscopedProvider: Provider[T]): Provider[T] = {
    new Provider[T] {
      def get: T = {
        val scopedObjects = getScopedObjectMap(key)
        val scopedObject = scopedObjects.get(key).asInstanceOf[T]
        if (scopedObject == null && !scopedObjects.containsKey(key)) {
          unscopedObject(key, unscopedProvider, scopedObjects)
        }
        else {
          scopedObject
        }
      }

      override def toString = {
        s"$unscopedProvider[$scopeName]"
      }
    }
  }

  override def toString = scopeName

  /* Public */

  /**
   * Start the 'request scope'
   * TODO (JIRA-474): assert local().isEmpty
   */
  def enter() {
    local.update(new JHashMap[Key[_], AnyRef]())
  }

  /**
   * Seed/Add an object into the 'request scope'
   *
   * @param value Value to seed/add into the request scope
   * @param overwrite Whether to overwrite an existing value already in the request scope (defaults to false)
   */
  def seed[T <: AnyRef : Manifest](value: T, overwrite: Boolean = false) {
    seed(
      Key.get(typeLiteral[T]),
      value,
      overwrite = overwrite)
  }

  /**
   * Seed/Add an object into the 'request scope'
   *
   * @param key Key of value to be added
   * @param value Value to seed/add into the request scope
   * @param overwrite Whether to overwrite an existing value already in the request scope (defaults to false)
   */
  def seed[T <: AnyRef](key: Key[T], value: T, overwrite: Boolean) {
    val scopedObjects = getScopedObjectMap(key)

    if (!overwrite) {
      assert(
        !scopedObjects.containsKey(key),
        "A value for the key " + key + " was already seeded in this scope. " +
          "Old value: " + scopedObjects.get(key) + " New value: " + value)
    }

    scopedObjects.put(key, value)
  }

  /** Exit the 'request scope' */
  def exit() {
    assert(local().isDefined, "No FinagleRequestScope in progress")
    local.clear()
  }

  /* Private */

  private def getScopedObjectMap(key: Key[_]) = {
    local() getOrElse {
      val lookupType = key.getTypeLiteral
      throw new OutOfScopeException(
        "Cannot access " + key + " outside of a FinagledScope.\n" +
          "Ensure that FinagleRequestScopeFilter is in your filter chain and FinagleRequestScopeModule is a loaded Guice module.\n" +
          "Ensure that the filter seeding " + lookupType + " is configured and in your filter chain.\n" +
          "Ensure that you're injecting Provider[" + lookupType + "] if injecting into a Singleton.\n" +
          "Ensure that you're calling provider.get every time (and not caching/storing the providers result in a class val)")
    }
  }

  // For details on CircularDependencyProxy, see https://github.com/google/guice/issues/843#issuecomment-54749202
  private def unscopedObject[T](key: Key[T], unscoped: Provider[T], scopedObjects: JMap[Key[_], Object]): T = {
    val unscopedObject = unscoped.get()

    // don't remember proxies; these exist only to serve circular dependencies
    if (!unscopedObject.isInstanceOf[CircularDependencyProxy]) {
      scopedObjects.put(key, unscopedObject.asInstanceOf[Object])
    }

    unscopedObject
  }
}
