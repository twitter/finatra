package com.twitter.finatra.http.tests.integration.doeverything.main;

import java.util.Collection;
import java.util.Collections;

import scala.collection.JavaConverters;
import scala.collection.Seq;

import com.google.inject.Module;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public final class TestModuleB extends TwitterModule {
  // FOR TESTING ONLY
  // https://twitter.github.io/finatra/user-guide/getting-started/flags.html#holding-a-reference
  private final Flag<Float> moduleBFlag;

  public TestModuleB() {
    this.moduleBFlag = createMandatoryFlag(
        /* name      = */ "moduleB.flag",
        /* help      = */ "help text",
        /* usage     = */ "usage text",
        /* flaggable = */ Flaggable.ofJavaFloat()
    );
  }

  @Override
  public Seq<Module> modules() {
    return JavaConverters.asScalaIteratorConverter(
        Collections.<Module>singletonList(
            new TestModuleA()).iterator()).asScala().toSeq();
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(
        new TestModuleC());
  }

  @Override
  public void configure() {
    // `moduleBFlag` has been parsed by the time configure() is called
    assert moduleBFlag.isDefined();
    assert moduleBFlag.apply() != null;
  }
}
