package com.twitter.finatra.http.modules

import com.twitter.inject.TwitterModule

object DocRootModule extends TwitterModule {

  // Only one of these flags should ever be set to a non-empty string as
  // these flags are mutually exclusive. Setting both will result in error.
  val localDocRoot = flag("local.doc.root", "", "File serving directory for local development")
  val docRoot = flag("doc.root", "", "File serving directory/namespace for classpath resources")
}
