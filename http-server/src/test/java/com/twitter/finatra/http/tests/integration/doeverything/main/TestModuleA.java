package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public final class TestModuleA extends TwitterModule {
  // FOR TESTING ONLY
  // https://twitter.github.io/finatra/user-guide/getting-started/flags.html#holding-a-reference
  private final Flag<Integer> moduleAFlag;

  public TestModuleA() {
    this.moduleAFlag = createMandatoryFlag(
        /* name      = */ "moduleA.flag",
        /* help      = */ "help text",
        /* usage     = */ "usage text",
        /* flaggable = */ Flaggable.ofJavaInteger()
    );
  }

  @Override
  public void configure() {
    // `moduleAFlag` has been parsed by the time configure() is called
    assert moduleAFlag.isDefined();
    assert moduleAFlag.apply() != null;
  }
}
