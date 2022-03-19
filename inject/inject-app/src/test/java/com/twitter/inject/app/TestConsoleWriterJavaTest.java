package com.twitter.inject.app;

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

public class TestConsoleWriterJavaTest extends Assert {

  @Test
  public void testOutDefaultCharset() {
    TestConsoleWriter console = new TestConsoleWriter();
    console.out().println("abc");
    assertEquals(console.inspectOut(), "abc\n");
  }

  @Test
  public void testOutSpecifiedCharset() {
    TestConsoleWriter console = new TestConsoleWriter();
    console.out().println("abc");
    assertEquals(console.inspectOut("UTF-8"), "abc\n");
  }

  @Test
  public void testErrDefaultCharset() {
    TestConsoleWriter console = new TestConsoleWriter();
    console.err().println("xyz");
    assertEquals(console.inspectErr(), "xyz\n");
  }

  @Test
  public void testErrSpecifiedCharset() {
    TestConsoleWriter console = new TestConsoleWriter();
    console.err().println("xyz");
    assertEquals(console.inspectErr("UTF-8"), "xyz\n");
  }

  @Test
  public void testReset() {
    TestConsoleWriter console = new TestConsoleWriter();
    console.out().println("abc");
    console.err().println("xyz");
    assertEquals(console.inspectOut(), "abc\n");
    assertEquals(console.inspectErr(), "xyz\n");
    console.reset();
    assertEquals(console.inspectOut(), "");
    assertEquals(console.inspectErr(), "");
    console.out().println("123");
    assertEquals(console.inspectOut(), "123\n");
    assertEquals(console.inspectErr(), "");
    console.err().println("987");
    assertEquals(console.inspectOut(), "123\n");
    assertEquals(console.inspectErr(), "987\n");
  }

  @Test
  public void testCustomByteArrayConstructor() {
    TestConsoleWriter console =
      new TestConsoleWriter(new ByteArrayOutputStream(1024), new ByteArrayOutputStream(1024));
    console.out().println("abc");
    console.err().println("xyz");
    assertEquals(console.inspectOut(), "abc\n");
    assertEquals(console.inspectErr(), "xyz\n");
  }

}
