package com.twitter.inject.requestscope

import com.twitter.inject.requestscope.FinagleRequestScope._

import com.google.inject.{
  Key,
  OutOfScopeException,
  Provider,
  Scope,
  Scopes
}
import com.twitter.finagle.context.Contexts
import com.twitter.inject.Logging
import java.util.{HashMap => JHashMap}
import net.codingwell.scalaguice.typeLiteral

object FinagleRequestScope {

  /** LocalContext Key */
  private val localKey = new Contexts.local.Key[JHashMap[Key[_], AnyRef]]()
  private val scopeName = "FinagleRequestScope"
}

/**
 * A Guice Custom Scope implemented with [[com.twitter.util.Local]] to mimic the behavior of the
 * '@RequestScoped' scope using [[com.twitter.util.Local]] to work within the context of
 * [[com.twitter.util.Future]]s.
 *
 * @note It is expected that users use this in combination with the typed [[FinagleRequestScopeFilter]] or the
 * type agnostic [[FinagleRequestScopeFilter.TypeAgnostic]].
 *
 * @see [[https://github.com/google/guice/wiki/Scopes#scopes Guice Scopes]]
 * @see [[https://github.com/google/guice/wiki/CustomScopes Guice Custom Scopes]]
 * @see [[https://twitter.github.io/finatra/user-guide/http/filters.html HttpServer Request Scoping]]
 * @see [[https://twitter.github.io/finatra/user-guide/thrift/filters.html#request-scope ThriftServer Request Scoping]]
 */
class FinagleRequestScope extends Scope with Logging {

  /* Public */

  /**
   * Initializes the value of the RequestScope to a new empty [[java.util.HashMap]] only for the scope of the
   * current [[com.twitter.finagle.context.LocalContext]] for the given function `fn`.
   *
   * @param fn the function to execute with the given LocalContext.
   * @return the result of executing the function `fn`.
   */
  protected[requestscope] def let[R](fn: => R): R = {
    Contexts.local.let(localKey, new JHashMap[Key[_], AnyRef]())(fn)
  }

  /**
   * Seed/Add an object into the 'request scope'.
   *
   * @param value Value to seed/add into the request scope
   * @param overwrite Whether to overwrite an existing value already in the request scope (defaults to false)
   */
  def seed[T <: AnyRef: Manifest](value: T, overwrite: Boolean = false): Unit = {
    seed(Key.get(typeLiteral[T]), value, overwrite = overwrite)
  }

  /**
   * Seed/Add an object into the 'request scope'.
   *
   * @param key Key of value to be added
   * @param value Value to seed/add into the request scope
   * @param overwrite Whether to overwrite an existing value already in the request scope (defaults to false)
   */
  def seed[T <: AnyRef](key: Key[T], value: T, overwrite: Boolean): Unit = {
    val scopedObjects = getScopedObjectMap(key)

    if (!overwrite) {
      assert(
        !scopedObjects.containsKey(key),
        "A value for the key " + key + " was already seeded in this scope. " +
          "Old value: " + scopedObjects.get(key) + " New value: " + value
      )
    }

    scopedObjects.put(key, value)
  }

  override def scope[T](key: Key[T], unscopedProvider: Provider[T]): Provider[T] = new Provider[T] {
    def get: T = {
      val scopedObjects = getScopedObjectMap(key)
      val scopedObject = scopedObjects.get(key).asInstanceOf[T]
      if (scopedObject == null && !scopedObjects.containsKey(key)) {
        unscopedObject(key, unscopedProvider, scopedObjects)
      } else {
        scopedObject
      }
    }

    override def toString: String = s"$unscopedProvider[$scopeName]"
  }

  override def toString: String = scopeName

  /* Private */

  private[this] def getScopedObjectMap(key: Key[_]): JHashMap[Key[_], AnyRef] = {
    Contexts.local.get(localKey).getOrElse {
      val lookupType = key.getTypeLiteral
      throw new OutOfScopeException(
        "Cannot access " + key + " outside of a FinagledScope.\n" +
          "Ensure that the FinagleRequestScopeFilter is in your filter chain and the FinagleRequestScopeModule is a loaded Module.\n" +
          "Ensure that the filter seeding " + lookupType + " is configured and in your filter chain.\n" +
          "Ensure that you're injecting Provider[" + lookupType + "] if injecting into a Singleton.\n" +
          "Ensure that you're calling provider.get every time (and not caching/storing the providers result in a class val)"
      )
    }
  }

  // For details on CircularDependencyProxy, see https://github.com/google/guice/issues/843#issuecomment-54749202
  private[this] def unscopedObject[T](
    key: Key[T],
    unscoped: Provider[T],
    scopedObjects: JHashMap[Key[_], Object]
  ): T = {
    val current = unscoped.get()

    // don't remember proxies; these exist only to serve circular dependencies
    if (!Scopes.isCircularProxy(current)) {
      scopedObjects.put(key, current.asInstanceOf[Object])
    }

    current
  }
}
