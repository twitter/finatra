package com.twitter.inject

import com.google.inject.{Key, Module}
import net.codingwell.scalaguice._

/**
 * Guice/twitter.util.Flag integrations usable from both non-private and private Guice modules
 */
trait TwitterBaseModule
  extends TwitterModuleFlags
  with TwitterModuleLifecycle {

  /** Additional modules to be composed into this module
    * NOTE: This Seq of modules is used instead of the standard Guice 'install' method */
  protected[inject] def modules: Seq[Module] = Seq()

  protected def createKey[T: Manifest] = {
    Key.get(typeLiteral[T])
  }
}
