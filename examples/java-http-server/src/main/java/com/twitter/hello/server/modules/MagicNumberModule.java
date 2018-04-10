package com.twitter.hello.server.modules;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public class MagicNumberModule extends TwitterModule {

  private final Flag<Integer> magicNumber = createFlag("module.magic.number", 137,
      "This is a magic number.", Flaggable.ofJavaInteger());

  private final Flag<Float> magicFloatingNumber = createFlag("module.magic.float", 3.1459f,
      "This is a magic floating point number.", Flaggable.ofJavaFloat());
}
