package com.twitter.finatra.tests.utils

import com.twitter.finatra.modules.InMemoryCredentialsModule
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector

class InMemoryCredentialsIntegrationTest extends IntegrationTest {
  val credentialsMap = Map(
    "username" -> "foo",
    "password" -> "bar")

  override def injector =
    TestInjector(new InMemoryCredentialsModule(credentialsMap))

  "InMemoryCredentialsModule" should {

    "load credentials" in {
      val credentials = injector.instance[Credentials]
      credentials.isEmpty should be(false)
      credentials.get("username").get should be("foo")
      credentials.get("password").get should be("bar")
    }
  }
}
