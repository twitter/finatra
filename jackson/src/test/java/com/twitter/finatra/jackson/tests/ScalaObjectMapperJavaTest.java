package com.twitter.finatra.jackson.tests;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finatra.jackson.ScalaObjectMapper;
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule;
import com.twitter.inject.Injector;
import com.twitter.inject.app.TestInjector;

public class ScalaObjectMapperJavaTest extends Assert {

  private final ScalaObjectMapperModule mapperModule = ScalaObjectMapperModule.get();

  /* Class under test */
  private final ScalaObjectMapper mapper = ScalaObjectMapper.apply();
  /* Test Injector */
  private final Injector injector =
      TestInjector.apply(Collections.singletonList(mapperModule)).create();

  @Test
  public void testConstructors() {
    Assert.assertNotNull(ScalaObjectMapper.apply());
    Assert.assertNotNull(ScalaObjectMapper.apply((com.google.inject.Injector) null));
    Assert.assertNotNull(ScalaObjectMapper.apply(injector.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.apply(mapper.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.apply(null, mapper.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.apply(injector.underlying(), mapper.underlying()));
  }

  @Test
  public void testModuleConstructors() {
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper());
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(null));
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(null, mapper.underlying()));
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(
        injector.underlying(), mapper.underlying()));

    Assert.assertNotNull(mapperModule.objectMapper());
    Assert.assertNotNull(mapperModule.objectMapper(null));
    Assert.assertNotNull(mapperModule.objectMapper(injector.underlying()));

    Assert.assertNotNull(mapperModule.camelCaseObjectMapper());
    Assert.assertNotNull(mapperModule.snakeCaseObjectMapper());
  }

  @Test
  public void testBuilderConstructors() {
    Assert.assertNotNull(ScalaObjectMapper.builder().jacksonScalaObjectMapper());
    Assert.assertNotNull(ScalaObjectMapper.builder().jacksonScalaObjectMapper(null));
    Assert.assertNotNull(ScalaObjectMapper.builder().jacksonScalaObjectMapper(
        null, mapper.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.builder().jacksonScalaObjectMapper(
        injector.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.builder().jacksonScalaObjectMapper(
        injector.underlying(), mapper.underlying()));

    Assert.assertNotNull(ScalaObjectMapper.builder().objectMapper());
    Assert.assertNotNull(ScalaObjectMapper.builder().objectMapper(null));
    Assert.assertNotNull(ScalaObjectMapper.builder().objectMapper(
        null, mapper.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.builder().objectMapper(
        injector.underlying()));
    Assert.assertNotNull(ScalaObjectMapper.builder().objectMapper(
        injector.underlying(), mapper.underlying()));

    Assert.assertNotNull(ScalaObjectMapper.builder().camelCaseObjectMapper());
    Assert.assertNotNull(ScalaObjectMapper.builder().snakeCaseObjectMapper());
  }
}
