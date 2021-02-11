package com.twitter.finatra.http.tests.integration.doeverything.main;

import com.google.inject.Module;
import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

import java.util.ArrayList;
import java.util.Collection;

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
  public Collection<Module> javaModules() {
    ArrayList<Module> modules = new ArrayList<>();
    modules.add(new TestModuleA());
    modules.add(new TestModuleC());
    return modules;
  }

  @Override
  public void configure() {
    // `moduleBFlag` has been parsed by the time configure() is called
    assert moduleBFlag.isDefined();
    assert moduleBFlag.apply() != null;
  }
}
