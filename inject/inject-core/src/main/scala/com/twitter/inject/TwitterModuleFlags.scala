package com.twitter.inject

import com.twitter.app.{Flag, Flaggable}
import scala.collection.mutable.ArrayBuffer

/**
 * Guice/twitter.util.Flag integrations
 */
trait TwitterModuleFlags {

  /* Mutable State */
  protected[inject] val flags = ArrayBuffer[Flag[_]]()

  /* Protected */

  /** Create a flag and add it to the modules flags list */
  protected def flag[T: Flaggable](name: String, default: T, help: String): Flag[T] = {
    val flag = new Flag[T](name, help, default)
    flags += flag
    flag
  }

  protected def flag[T: Flaggable: Manifest](name: String, help: String): Flag[T] = {
    val flag = new Flag[T](name, help, manifest[T].runtimeClass.toString)
    flags += flag
    flag
  }
}
