package com.twitter.finatra.example.modules;

import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;

public final class MagicNumberModule extends TwitterModule {

  public MagicNumberModule() {
    createFlag(
        /* name      = */ "module.magic.number",
        /* default   = */ 137,
        /* help      = */ "This is a magic number.",
        /* flaggable = */ Flaggable.ofJavaInteger());

    createFlag(
        /* name      = */ "module.magic.float",
        /* default   = */ 3.1459f,
        /* default   = */ "This is a magic floating point number.",
        /* flaggable = */ Flaggable.ofJavaFloat());
  }
}
