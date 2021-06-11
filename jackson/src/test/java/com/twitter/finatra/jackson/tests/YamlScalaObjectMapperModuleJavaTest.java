package com.twitter.finatra.jackson.tests;

import java.util.Collections;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finatra.jackson.modules.YamlScalaObjectMapperModule;
import com.twitter.inject.Injector;
import com.twitter.inject.app.TestInjector;

public class YamlScalaObjectMapperModuleJavaTest extends Assert {

  private final YamlScalaObjectMapperModule mapperModule = YamlScalaObjectMapperModule.get();

  /* Class under test */
  /* Test Injector */
  private final Injector injector =
      TestInjector.apply(Collections.singletonList(mapperModule)).create();

  @Test
  public void testModuleConstructors() {
    //jacksonScalaObjectMapper
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper());
    Assert.assertTrue(mapperModule.jacksonScalaObjectMapper().getFactory() instanceof YAMLFactory);
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(injector.underlying()));
    Assert.assertTrue(
        mapperModule.jacksonScalaObjectMapper(
            injector.underlying()).getFactory() instanceof YAMLFactory);
    Assert.assertNotNull(mapperModule.jacksonScalaObjectMapper(null));
    Assert.assertTrue(
        mapperModule.jacksonScalaObjectMapper(null).getFactory() instanceof YAMLFactory);

    //objectMapper
    Assert.assertNotNull(mapperModule.objectMapper());
    Assert.assertTrue(
        mapperModule.objectMapper().underlying().getFactory() instanceof YAMLFactory);
    Assert.assertNotNull(mapperModule.objectMapper(injector.underlying()));
    Assert.assertTrue(
        mapperModule.objectMapper(
            injector.underlying()).underlying().getFactory() instanceof YAMLFactory);
    Assert.assertNotNull(mapperModule.objectMapper(null));
    Assert.assertTrue(
        mapperModule.objectMapper(null).underlying().getFactory() instanceof YAMLFactory);

    // mappers for property naming strategies
    Assert.assertNotNull(mapperModule.camelCaseObjectMapper());
    Assert.assertTrue(
        mapperModule.camelCaseObjectMapper().underlying().getFactory() instanceof YAMLFactory);
    Assert.assertNotNull(mapperModule.snakeCaseObjectMapper());
    Assert.assertTrue(
        mapperModule.snakeCaseObjectMapper().underlying().getFactory() instanceof YAMLFactory);
  }
}
