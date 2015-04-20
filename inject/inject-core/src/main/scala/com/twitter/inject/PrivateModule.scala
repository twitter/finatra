package com.twitter.inject

import com.google.inject.{PrivateModule => GuicePrivateModule}
import net.codingwell.scalaguice._

abstract class PrivateModule
  extends GuicePrivateModule
  with ScalaPrivateModule {

  /* Overrides */

  // Provide default configure method so Module's using only @Provider don't need an empty configure method
  override protected def configure() {}
}