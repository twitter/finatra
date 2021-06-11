package com.twitter.finatra.jackson.tests;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule;
import com.twitter.inject.Injector;
import com.twitter.inject.app.TestInjector;

public class ScalaObjectMapperModuleJavaTest extends Assert {

  private final ScalaObjectMapperModule mapperModule = ScalaObjectMapperModule.get();

  /* Class under test */
  /* Test Injector */
  private final Injector injector =
      TestInjector.apply(Collections.singletonList(mapperModule)).create();

  @Test
  public void testModuleConstructors() {
    //jacksonScalaObjectMapper
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper());
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(injector.underlying()));
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(null));

    //objectMapper
    Assert.assertNotNull(mapperModule.objectMapper());
    Assert.assertNotNull(mapperModule.objectMapper(injector.underlying()));
    Assert.assertNotNull(mapperModule.objectMapper(null));

    // mappers for property naming strategies
    Assert.assertNotNull(mapperModule.camelCaseObjectMapper());
    Assert.assertNotNull(mapperModule.snakeCaseObjectMapper());
  }
}
