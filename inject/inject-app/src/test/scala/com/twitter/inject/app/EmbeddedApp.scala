package com.twitter.inject.app

import com.google.inject.Module
import com.twitter.inject.Injector
import com.twitter.util.logging.Logging
import java.lang.annotation.Annotation
import scala.annotation.varargs
import scala.collection.JavaConverters._

/**
 * EmbeddedApp allow's a [[com.twitter.inject.app.App]] to be integration and
 * feature tested.
 *
 * @param app The [[com.twitter.inject.app.App]] to be started for testing
 */
class EmbeddedApp(app: com.twitter.inject.app.App) extends BindDSL with Logging {

  /** Note the Injector is ONLY available AFTER app.main() has been called */
  lazy val injector: Injector = app.injector

  def underlying: com.twitter.inject.app.App = app

  /** Run the underlying App main with the given `Map[String, Any]` passed as application flags */
  def main(flags: Map[String, Any]): Unit = {
    val stringArgs = flagsAsArgs(flags)
    info("Calling main with args: " + stringArgs.mkString(" "))
    app.nonExitingMain(stringArgs.toArray)
  }

  /** Run the underlying App main with the given sequence of tuples passed as application flags */
  def main(flags: (String, Any)*): Unit = {
    main(flags.toMap)
  }

  /** Convenience to run the underlying App main with no arguments */
  def main(): Unit = {
    main(Map[String, Any]())
  }

  /** Run the underlying App main with the given `Map[String, Any]` and sequence of String concatenated and passed as application flags */
  def main(flags: Map[String, Any], args: Seq[String]): Unit = {
    val stringArgs = flagsAsArgs(flags) ++ args
    info("Calling main with args: " + stringArgs.mkString(" "))
    app.nonExitingMain(stringArgs.toArray)
  }

  /* Java friendly */
  @varargs def main(flags: java.util.Map[String, Any], args: String*): Unit =
    main(flags = flags.asScala.toMap, args = args)

  def main(flags: java.util.Map[String, Any]): Unit =
    main(flags = flags.asScala.toMap)

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], instance: T): this.type =
    super.bindClass[T](clazz, instance)

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): this.type =
    super.bindClass[T](clazz, annotation, instance)

  // java-forwarder methods
  override final def bindClass[T, Ann <: Annotation](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instance: T
  ): this.type =
    super.bindClass[T, Ann](clazz, annotationClazz, instance)

  // java-forwarder methods
  override final def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): this.type =
    super.bindClass[T, U](clazz, instanceClazz)

  // java-forwarder methods
  override final def bindClass[T, U <: T](
    clazz: Class[T],
    annotation: Annotation,
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, U](clazz, annotation, instanceClazz)

  // java-forwarder methods
  override final def bindClass[T, Ann <: Annotation, U <: T](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, Ann, U](clazz, annotationClazz, instanceClazz)

  /* Protected */

  override final protected def addInjectionServiceModule(module: Module): Unit = {
    app.addFrameworkOverrideModules(module)
  }

  /* Private */

  private[this] def flagsAsArgs(flags: Map[String, Any]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }
}
