package com.twitter.finatra.mustache.modules

import com.twitter.inject.TwitterModule

object MustacheFlagsModule extends TwitterModule {

  /** Defines the location of where to resolve mustache templates */
  flag(MustacheFlags.TemplatesDirectory, "templates", "templates resource directory")
}
