package com.twitter.inject.app.tests

import com.twitter.inject.Test
import com.twitter.inject.app.TestConsoleWriter
import java.io.ByteArrayOutputStream

class TestConsoleWriterTest extends Test {

  test("reads out (default charset)") {
    val console = new TestConsoleWriter()
    console.out.println("abc")
    assert(console.inspectOut() == "abc\n")
  }

  test("reads out (specified charset)") {
    val console = new TestConsoleWriter()
    console.out.println("abc")
    assert(console.inspectOut("UTF-8") == "abc\n")
  }

  test("reads err (default charset)") {
    val console = new TestConsoleWriter()
    console.err.println("xyz")
    assert(console.inspectErr() == "xyz\n")
  }

  test("reads err (specified charset)") {
    val console = new TestConsoleWriter()
    console.err.println("xyz")
    assert(console.inspectErr("UTF-8") == "xyz\n")
  }

  test("can reset and re-use") {
    val console = new TestConsoleWriter()
    console.out.println("abc")
    console.err.println("xyz")
    assert(console.inspectOut() == "abc\n")
    assert(console.inspectErr() == "xyz\n")
    console.reset()
    assert(console.inspectOut() == "")
    assert(console.inspectErr() == "")
    console.out.println("123")
    assert(console.inspectOut() == "123\n")
    assert(console.inspectErr() == "")
    console.err.println("987")
    assert(console.inspectOut() == "123\n")
    assert(console.inspectErr() == "987\n")
  }

  test("can specify larger byte array output stream") {
    val console =
      new TestConsoleWriter(new ByteArrayOutputStream(1024), new ByteArrayOutputStream(1024))
    console.out.println("abc")
    console.err.println("xyz")
    assert(console.inspectOut() == "abc\n")
    assert(console.inspectErr() == "xyz\n")
  }

}
