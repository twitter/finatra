package com.twitter.inject

import com.google.inject.{Key, Module}
import net.codingwell.scalaguice._
import scala.reflect.runtime.universe.TypeTag

/**
 * Guice/twitter.util.Flag integrations usable from both non-private and private Guice modules
 */
trait TwitterBaseModule extends TwitterModuleFlags with TwitterModuleLifecycle {

  /**
   * Additional modules to be composed into this module
   *
   * NOTE: This Seq of modules is generally used instead of the standard Guice 'install' method so that
   * TwitterModules with flag definitions can be supported.
   *
   * However, AbstractModule.install can still be used for non-TwitterModules, and is sometimes preferred
   * due to install being deferred until after flag parsing occurs.
   */
  protected[inject] def modules: Seq[Module] = Seq()

  /**
   * Additional framework modules to be composed into this module
   */
  protected[inject] def frameworkModules: Seq[Module] = Seq()

  protected def createKey[T: TypeTag]: Key[T] = {
    Key.get(typeLiteral[T])
  }
}
