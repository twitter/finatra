package com.twitter.inject

import com.google.inject.PrivateModule
import net.codingwell.scalaguice.ScalaPrivateModule

/**
 * A module whose configuration information is hidden from its environment by default. Only bindings
 * that are explicitly exposed will be available to other modules and to the users of the injector.
 * This module may expose the bindings it creates and the bindings of the modules it installs.
 *
 * @note Calling [[https://static.javadoc.io/com.google.inject/guice/4.1.0/com/google/inject/PrivateModule.html#install-com.google.inject.Module- com.google.inject.PrivateModule#install]]
 *       in the [[configure()]] method is not supported. Please set [[TwitterBaseModule.modules]]
 *       (or [[TwitterBaseModule.javaModules]]) to a non-empty list instead.
 * @see [[https://static.javadoc.io/com.google.inject/guice/4.1.0/com/google/inject/PrivateModule.html com.google.inject.PrivateModule]]
 * @see [[https://twitter.github.io/finatra/user-guide/getting-started/modules.html Writing Modules in Finatra]]
 */
abstract class TwitterPrivateModule
    extends PrivateModule
    with TwitterBaseModule
    with ScalaPrivateModule {

  /* Overrides */

  /**
   * Configures a [[https://google.github.io/guice/api-docs/4.1/javadoc/com/google/inject/Binder.html com.google.inject.Binder]]
   * via the exposed methods. A default implementation is provided such that extensions using
   * only `@Provider`-annotated methods do not need to implement an empty
   * [[TwitterModule.configure()]] method.
   *
   * @see [[com.google.inject.PrivateModule#configure]]
   * @see [[https://static.javadoc.io/com.google.inject/guice/4.1.0/com/google/inject/PrivateModule.html#configure-- com.google.inject.PrivateModule#configure()]]
   */
  override protected def configure(): Unit = {}
}
