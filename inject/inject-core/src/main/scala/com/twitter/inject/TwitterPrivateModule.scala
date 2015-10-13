package com.twitter.inject

import com.google.inject.{PrivateModule => GuicePrivateModule}
import net.codingwell.scalaguice._

/*
 * Note: Calling install in the configure method is not supported. Please use 'override val modules = Seq(module1, module2, ...)' instead
 */
abstract class TwitterPrivateModule
  extends GuicePrivateModule
  with TwitterBaseModule
  with ScalaPrivateModule {

  /* Overrides */

  // Provide default configure method so Module's using only @Provider don't need an empty configure method
  override protected def configure() {}
}