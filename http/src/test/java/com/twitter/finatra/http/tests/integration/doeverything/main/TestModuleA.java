package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public class TestModuleA extends TwitterModule {

  private Flag<Integer> moduleAFlag = createMandatoryFlag(
      "moduleA.flag",
      "help text",
      "usage text",
      Flaggable.ofJavaInteger()
  );

  @Override
  public void configure() {
    assert moduleAFlag.isDefined();
    assert moduleAFlag.apply() != null;
  }
}
