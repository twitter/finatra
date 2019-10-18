package com.twitter.inject

import com.google.inject.Module

trait TwitterBaseModule extends TwitterModuleFlags with TwitterModuleLifecycle {

  /**
   * Additional modules to be composed into this module. This list of modules is generally used
   * ''instead'' of the [[TwitterModule.install]] method to properly support the
   * [[TwitterModuleLifecycle]] for [[TwitterModule]] instances.
   *
   * @note [[TwitterModule.install(module: Module)]] can still be used for non-[[TwitterModule]] 1
   *      instances, and is sometimes preferred due to `install` being deferred until after flag parsing occurs.
   * @note Java users should prefer [[javaModules]].
   */
  protected[inject] def modules: Seq[Module] = Seq()

  /** Additional modules to be composed into this module from Java */
  protected[inject] def javaModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /** Additional framework modules to be composed into this module. */
  protected[inject] def frameworkModules: Seq[Module] = Seq()
}
