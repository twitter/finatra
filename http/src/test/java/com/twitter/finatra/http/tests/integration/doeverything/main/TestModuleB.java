package com.twitter.finatra.http.tests.integration.doeverything.main;

import java.util.ArrayList;
import java.util.Collection;

import scala.collection.JavaConverters;
import scala.collection.Seq;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public class TestModuleB extends TwitterModule {
  private Flag<Float> moduleBFlag = createMandatoryFlag(
      "moduleB.flag",
      "help text",
      "usage text",
      Flaggable.ofJavaFloat()
  );

  @Override
  public Seq<Module> modules() {
    ArrayList<Module> m = new ArrayList<>();
    m.add(new TestModuleA());
    return JavaConverters.asScalaIteratorConverter(m.iterator()).asScala().toSeq();
  }

  @Override
  public Collection<Module> javaModules() {
    return ImmutableList.<Module>of(
        new TestModuleC());
  }

  @Override
  public void configure() {
    assert moduleBFlag.isDefined();
    assert moduleBFlag.apply() != null;
  }
}
