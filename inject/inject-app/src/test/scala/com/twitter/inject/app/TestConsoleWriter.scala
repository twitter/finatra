package com.twitter.inject.app

import com.twitter.inject.app.console.ConsoleWriter
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * A utility that can be injected into an [[App]] to inspect console output.
 *
 * @example
 *   {{{
 *     val console = new TestConsoleWriter()
 *     val app = new EmbeddedApp(myApp).bind[ConsoleWriter].toInstance(console)
 *
 *     app.main()
 *
 *     assert(console.inspectOut() == "abc")
 *     assert(console.inspectErr() == "")
 *   }}}
 *
 * @note This utility is designed for use with an [[App]] that is a command-line utility and is
 *       NOT meant for use with a `TwitterServer` or inspecting [[com.twitter.util.logging.Logger]]
 *       output. If you have a command-line utility that is designed to be piped to another
 *       command-line utility (i.e. via a [[Unix Pipeline https://en.wikipedia.org/wiki/Pipeline_(Unix)]])
 *       - this utility is for you!
 */
class TestConsoleWriter(outSpy: ByteArrayOutputStream, errSpy: ByteArrayOutputStream)
    extends ConsoleWriter(new PrintStream(outSpy), new PrintStream(errSpy)) {

  def this() = this(new ByteArrayOutputStream(), new ByteArrayOutputStream())

  /** Retrieve the String contents of the [[out]] buffer with the default system character encoding. */
  def inspectOut(): String = outSpy.toString()

  /** Retrieve the String contents of the [[out]] buffer by specified character encoding. */
  def inspectOut(charsetName: String): String = outSpy.toString(charsetName)

  /** Retrieve the String contents of the [[err]] buffer with the default system character encoding. */
  def inspectErr(): String = errSpy.toString()

  /** Retrieve the String contents of the [[err]] buffer by specified character encoding. */
  def inspectErr(charsetName: String): String = errSpy.toString(charsetName)

  /** Reset the contents of both the [[out]] and [[err]] buffers. */
  def reset(): Unit = {
    outSpy.reset()
    errSpy.reset()
  }

}
