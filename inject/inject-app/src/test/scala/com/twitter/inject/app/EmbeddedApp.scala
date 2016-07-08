package com.twitter.inject.app

import com.twitter.inject.Logging

/**
 * EmbeddedApp allow's a [[com.twitter.inject.app.App]] to be integration and
 * feature tested.
 *
 * @param app The [[com.twitter.inject.app.App]] to be started for testing
 */
class EmbeddedApp(
  app: com.twitter.inject.app.App) extends Logging {

  def bind[T : Manifest](instance: T): EmbeddedApp = {
    app.addFrameworkOverrideModules(new InjectionServiceModule(instance))
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

  private def flagsAsArgs(flags: Map[String, Any]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }
}


