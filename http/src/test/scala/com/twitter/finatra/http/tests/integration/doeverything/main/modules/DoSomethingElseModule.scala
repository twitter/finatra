package com.twitter.finatra.http.tests.integration.doeverything.main.modules

import com.twitter.app.Flag
import com.twitter.inject.TwitterModule

class DoSomethingElseModule extends TwitterModule {

  private val someFlag: Flag[String] = flag[String]("something.flag", "Module Magic number")

  override protected def configure(): Unit = {
    assert(someFlag.isDefined)
    assert(someFlag() != null)
  }
}
