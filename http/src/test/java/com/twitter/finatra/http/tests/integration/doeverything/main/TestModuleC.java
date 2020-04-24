package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public final class TestModuleC extends TwitterModule {
  // FOR TESTING ONLY
  // https://twitter.github.io/finatra/user-guide/getting-started/flags.html#holding-a-reference
  private final Flag<String> moduleCFlag;

  public TestModuleC() {
    this.moduleCFlag = createMandatoryFlag(
        /* name      = */ "moduleC.flag",
        /* help      = */ "help text",
        /* usage     = */ "usage text",
        /* flaggable = */ Flaggable.ofString()
    );
  }

  @Override
  public void configure() {
    // `moduleCFlag` has been parsed by the time configure() is called
    assert moduleCFlag.isDefined();
    assert moduleCFlag.apply() != null;
  }
}
