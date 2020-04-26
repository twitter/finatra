package com.twitter.hello.server.modules;

import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public class MagicNumberModule extends TwitterModule {

  public MagicNumberModule() {
    createFlag("module.magic.number", 137,
        "This is a magic number.", Flaggable.ofJavaInteger());

    createFlag("module.magic.float", 3.1459f,
        "This is a magic floating point number.", Flaggable.ofJavaFloat());
  }
}
