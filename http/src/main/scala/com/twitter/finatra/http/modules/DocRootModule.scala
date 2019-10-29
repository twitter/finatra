package com.twitter.finatra.http.modules

import com.twitter.app.Flag
import com.twitter.inject.TwitterModule

@deprecated("Use the com.twitter.finatra.modules.FileResolverModule", "2019-10-26")
object DocRootModule extends TwitterModule {

  // Only one of these flags should ever be set to a non-empty string as
  // these flags are mutually exclusive. Setting both will result in error.
  val localDocRoot: Flag[String] =
    flag("local.doc.root", "", "File serving directory for local development")
  val docRoot: Flag[String] =
    flag("doc.root", "", "File serving directory/namespace for classpath resources")
}
