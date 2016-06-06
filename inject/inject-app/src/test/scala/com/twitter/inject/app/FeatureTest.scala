package com.twitter.inject.app

import com.twitter.inject.{Injector, IntegrationTest, Test}

trait FeatureTest extends Test with IntegrationTest {

  protected def app: EmbeddedApp

  override protected def injector: Injector = app.injector

  override protected def beforeAll() {
    if (app.isStarted && hasBoundFields) {
      throw new Exception("ERROR: App started before integrationTestModule added. " +
        "@Bind will not work unless references to the app/server are lazy, or within a ScalaTest " +
        "lifecycle method or test method, or the integrationTestModule is manually added as " +
        "an override module.")
    }

    assert(app.isInjectableApp)
    app.injectableApp.addFrameworkOverrideModules(integrationTestModule)
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    app.close()
  }
}