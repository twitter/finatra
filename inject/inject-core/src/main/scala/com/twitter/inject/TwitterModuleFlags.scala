package com.twitter.inject

import com.twitter.app.{Flag, Flaggable}
import scala.collection.mutable.ArrayBuffer

/**
 * Guice/twitter.util.Flag integrations usable from both non-private and private Guice modules
 */
trait TwitterModuleFlags {

  /* Mutable State */

  protected[inject] val flags: ArrayBuffer[Flag[_]] = ArrayBuffer[Flag[_]]()

  /* Protected */

  /**
   * A Java-friendly method for creating a named [[Flag]].
   *
   * @param name the name of the [[Flag]].
   * @param default a default value for the [[Flag]] when no value is given as an application
   *                argument.
   * @param help the help text explaining the purpose of the [[Flag]].
   * @return the created [[Flag]].
   */
  protected def createFlag[T](name: String, default: T, help: String, flaggable: Flaggable[T]): Flag[T] = {
    implicit val implicitFlaggable: Flaggable[T] = flaggable
    val flag = new Flag[T](name, help, default) {
      override protected val flaggable: Flaggable[T] = implicitFlaggable
    }
    flags += flag
    flag
  }

  /**
   * A Java-friendly way to create a "mandatory" [[Flag]]. "Mandatory" flags MUST have a value
   * provided as an application argument (as they have no default value to be used).
   *
   * @param name the name of the [[Flag]].
   * @param help the help text explaining the purpose of the [[Flag]].
   * @param usage a string describing the type of the [[Flag]], i.e.: Integer.
   * @return the created [[Flag]].
   */
  def createMandatoryFlag[T](name: String, help: String, usage: String, flaggable: Flaggable[T]): Flag[T] = {
    implicit val implicitFlaggable: Flaggable[T] = flaggable
    val flag = new Flag[T](name, help, usage) {
      override protected val flaggable: Flaggable[T] = implicitFlaggable
    }
    flags += flag
    flag
  }

  /**
   * Create a [[Flag]] and add it to this Module's flags list.
   *
   * @note Java users: see the more Java-friendly [[createFlag]] or [[createMandatoryFlag]].
   *
   * @param name the name of the [[Flag]].
   * @param default a default value for the [[Flag]] when no value is given as an application
   *                argument.
   * @param help the help text explaining the purpose of the [[Flag]].
   * @tparam T must be a [[Flaggable]] type.
   * @return the created [[Flag]].
   */
  protected def flag[T: Flaggable](name: String, default: T, help: String): Flag[T] = {
    val flag = new Flag[T](name, help, default)
    flags += flag
    flag
  }

  /**
   * Create a "mandatory" flag and add it to this Module's flags list."Mandatory" flags MUST have
   * a value provided as an application argument (as they have no default value to be used).
   *
   * @note Java users: see the more Java-friendly [[createFlag]] or [[createMandatoryFlag]].
   *
   * @param name the name of the [[Flag]].
   * @param help the help text explaining the purpose of the [[Flag]].
   * @tparam T must be a [[Flaggable]] type.
   * @return the created [[Flag]].
   */
  protected def flag[T: Flaggable: Manifest](name: String, help: String): Flag[T] = {
    val flag = new Flag[T](name, help, manifest[T].runtimeClass.toString)
    flags += flag
    flag
  }
}
