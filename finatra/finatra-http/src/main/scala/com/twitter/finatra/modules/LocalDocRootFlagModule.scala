package com.twitter.finatra.modules

import com.twitter.inject.TwitterModule

object LocalDocRootFlagModule extends TwitterModule {

  flag(
    "local.doc.root",
    "src/main/webapp/",
    "File serving directory when 'env' system property set to 'dev'")
}
