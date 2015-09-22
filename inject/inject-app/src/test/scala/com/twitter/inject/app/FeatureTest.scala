package com.twitter.inject.app

import com.twitter.inject.{Injector, IntegrationTest, Test}

trait FeatureTest extends Test with IntegrationTest {

  protected def app: EmbeddedApp

  override protected def injector: Injector = app.injector

  override protected def beforeAll() {
    assert(app.isGuiceApp)
    app.guiceApp.addFrameworkOverrideModules(integrationTestModule)
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    app.close()
  }
}