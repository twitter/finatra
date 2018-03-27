package com.twitter.calculator.modules;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.calculator.Mode;
import com.twitter.inject.TwitterModule;

public class ModeModule extends TwitterModule {

  private final Flag<String> mode = createFlag("calculator.mode", Mode.Standard.toString(),
      "Calculator mode.", Flaggable.ofString());

  @Override
  public void configure() {
    bind(Mode.class).toInstance(Mode.valueOf(mode.apply()));
  }
}
