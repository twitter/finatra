package com.twitter.inject.app.console

import java.io.PrintStream

/**
 * A test-friendly wrapper for the standard system output streams ([[System.out]], [[System.err]]).
 *
 * Ex:
 *
 * {{{
 *   class MyApp extends App {
 *     def run(): Unit = {
 *       val console = injector.instance[ConsoleWriter]
 *       console.out.println("Hello, World!")
 *     }
 *   }
 * }}}
 *
 * @note Using the ConsoleWriter is preferred for testing because its state can be locally
 *       overridden/bound, where as using [[System.setOut()]] or [[System.setErr()]]
 *       modifies global state of the JVM, which may result in flaky tests.
 */
class ConsoleWriter private[app] (val out: PrintStream, val err: PrintStream) {

  /**
   * Let this [[ConsoleWriter ConsoleWriter's]] [[out]] and [[error]] streams be used to scope
   * the scala [[Console]].
   *
   * @note This does not impact or change the global Java [[System.out]] or [[System.err]], nor does
   *       it currently impact the behavior of installed/configured
   *       [[com.twitter.util.logging.Logger Loggers]].
   */
  def let[T](f: => T): T = {
    Console.withOut(out) {
      Console.withErr(err) {
        f
      }
    }
  }

}
