package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public class TestModuleC extends TwitterModule {

  private Flag<String> moduleCFlag = createMandatoryFlag(
      "moduleC.flag",
      "help text",
      "usage text",
      Flaggable.ofString()
  );

  @Override
  public void configure() {
    assert moduleCFlag.isDefined();
    assert moduleCFlag.apply() != null;
  }
}
