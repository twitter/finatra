package com.twitter.inject.app

import com.twitter.inject.Logging
import java.lang.annotation.Annotation
import scala.reflect.runtime.universe._

/**
 * EmbeddedApp allow's a [[com.twitter.inject.app.App]] to be integration and
 * feature tested.
 *
 * @param app The [[com.twitter.inject.app.App]] to be started for testing
 */
class EmbeddedApp(app: com.twitter.inject.app.App) extends Logging {

  /**
   * Bind an instance of type [T] to the object graph of the underlying app.
   * This will REPLACE any previously bound instance of the given type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @return this [[EmbeddedApp]].
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests
   */
  def bind[T: TypeTag](instance: T): EmbeddedApp = {
    app.addFrameworkOverrideModules(new InjectionServiceModule[T](instance))
    this
  }

  /**
   * Bind an instance of type [T] annotated with Annotation type [A] to the object
   * graph of the underlying app. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @tparam A - type of the Annotation used to bind the instance.
   * @return this [[EmbeddedApp]].
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests
   */
  def bind[T: TypeTag, A <: Annotation: TypeTag](instance: T): EmbeddedApp = {
    app.addFrameworkOverrideModules(new InjectionServiceWithAnnotationModule[T, A](instance))
    this
  }

  def main(flags: Map[String, Any]): Unit = {
    val stringArgs = flagsAsArgs(flags)
    info("Calling main with args: " + stringArgs.mkString(" "))
    app.nonExitingMain(stringArgs.toArray)
  }

  def main(flags: (String, Any)*): Unit = {
    main(flags.toMap)
  }

  def main(): Unit = {
    main(Map[String, Any]())
  }

  def main(flags: Map[String, Any], args: Seq[String]): Unit = {
    val stringArgs = flagsAsArgs(flags) ++ args
    info("Calling main with args: " + stringArgs.mkString(" "))
    app.nonExitingMain(stringArgs.toArray)
  }

  /* Private */

  private[this] def flagsAsArgs(flags: Map[String, Any]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }
}
