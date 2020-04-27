package com.twitter.finatra.example.modules;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.finatra.example.Mode;
import com.twitter.inject.TwitterModule;

public final class ModeModule extends TwitterModule {
  // We hold a reference only to use it in the configure() to help bind
  // an instance of a Mode.
  // https://twitter.github.io/finatra/user-guide/getting-started/flags.html#holding-a-reference
  private final Flag<String> mode;

  public ModeModule() {
    this.mode = createFlag(
        /* name      = */ "calculator.mode",
        /* default   = */ Mode.Standard.toString(),
        /* help      = */ "Calculator mode.",
        /* flaggable = */ Flaggable.ofString());
  }


  @Override
  public void configure() {
    // `mode` has been parsed by the time configure() is called but you should be
    // cautious when holding a reference to a defined flag:
    // https://twitter.github.io/finatra/user-guide/getting-started/flags.html#holding-a-reference
    bind(Mode.class).toInstance(Mode.valueOf(mode.apply()));
  }
}
