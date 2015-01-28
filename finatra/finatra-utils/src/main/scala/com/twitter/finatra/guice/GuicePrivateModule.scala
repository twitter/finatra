package com.twitter.finatra.guice

import com.google.inject._
import net.codingwell.scalaguice._

abstract class GuicePrivateModule
  extends PrivateModule
  with ScalaPrivateModule {

  /* Overrides */

  // Provide default configure method so Module's using only @Provider don't need an empty configure method
  override protected def configure() {}
}