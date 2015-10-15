package com.twitter.inject.app

import com.twitter.inject.{Injector, IntegrationTest, Test}

trait FeatureTest extends Test with IntegrationTest {

  protected def app: EmbeddedApp

  override protected def injector: Injector = app.injector

  override protected def beforeAll() {
    if (app.isStarted) {
      warn("WARNING: App started before integrationTestModule added. " +
        "@Bind will not work unless integrationTestModule was manually added as an override module")
    }

    assert(app.isGuiceApp)
    app.guiceApp.addFrameworkOverrideModules(integrationTestModule)
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    app.close()
  }
}