package com.twitter.inject.app;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
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
}
