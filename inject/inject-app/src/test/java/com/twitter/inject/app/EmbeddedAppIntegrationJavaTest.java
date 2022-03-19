package com.twitter.inject.app;

import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.app.FlagParseException;
import com.twitter.inject.app.console.ConsoleWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class EmbeddedAppIntegrationJavaTest extends Assert {

  @Test
  public void testEmbeddedApp() {
    SampleJavaApp sampleJavaApp = new SampleJavaApp();

    EmbeddedApp app = new EmbeddedApp(sampleJavaApp);
    app.main();

    assertEquals(sampleJavaApp.getSampleServiceResponse(), "hi yo");
  }

  // -help arg exits with non-zero status/throws
  @Test(expected = Exception.class)
  public void testEmbeddedHelpArg() {
    SampleJavaApp sampleJavaApp = new SampleJavaApp();

    EmbeddedApp app = new EmbeddedApp(sampleJavaApp);
    app.main(ImmutableMap.of(), "-help");
  }

  // we are just testing that flags get passed, these will all be undefined and should throw
  @Test(expected = FlagParseException.class)
  public void testEmbeddedAppFailOnUndefinedFlags() {
    SampleJavaApp sampleJavaApp = new SampleJavaApp();

    EmbeddedApp app = new EmbeddedApp(sampleJavaApp);
    app.main(ImmutableMap.of("-flag1", "value1", "-flag2", "value2"));
  }

  @Test
  public void testEmbeddedAppWithBind() {
    SampleJavaAppService mockSampleJavaAppService = mock(SampleJavaAppService.class);
    when(mockSampleJavaAppService.sayHi(anyString())).thenReturn("hi mock");

    SampleJavaApp sampleJavaApp = new SampleJavaApp();

    EmbeddedApp app = new EmbeddedApp(sampleJavaApp);
    app.bindClass(SampleJavaAppService.class, mockSampleJavaAppService);
    app.main();

    assertEquals(sampleJavaApp.getSampleServiceResponse(), "hi mock");
    reset(mockSampleJavaAppService);
  }

  @Test
  public void testEmbeddedAppWithBindChaining() {
    SampleJavaAppService mockSampleJavaAppService = mock(SampleJavaAppService.class);
    when(mockSampleJavaAppService.sayHi(anyString())).thenReturn("hi mock");

    SampleJavaApp sampleJavaApp = new SampleJavaApp();

    EmbeddedApp app = new EmbeddedApp(sampleJavaApp)
        .bindClass(SampleJavaAppService.class, mockSampleJavaAppService);

    app.main();

    assertEquals(sampleJavaApp.getSampleServiceResponse(), "hi mock");
    reset(mockSampleJavaAppService);
  }

  @Test
  public void testEmbeddedAppWithTestConsoleWriter() {
    AbstractApp myApp = new AbstractApp() {
      @Override
      public void run() {
        ConsoleWriter console = injector().instance(ConsoleWriter.class);
        console.out().println("Hello, World!");
        console.err().println("Oh, No!");
        System.out.println("This doesn't get captured");
        System.err.println("This also does not get captured");
        info(() -> "this will not be captured");
        error(() -> "this will also not be captured");
      }
    };

    TestConsoleWriter testConsole = new TestConsoleWriter();
    EmbeddedApp app = new EmbeddedApp(myApp);
    app.bindClass(ConsoleWriter.class, testConsole);

    assertEquals(testConsole.inspectOut(), "");
    assertEquals(testConsole.inspectErr(), "");

    app.main();

    assertEquals(testConsole.inspectOut(), "Hello, World!\n");
    assertEquals(testConsole.inspectErr(), "Oh, No!\n");
  }
}
