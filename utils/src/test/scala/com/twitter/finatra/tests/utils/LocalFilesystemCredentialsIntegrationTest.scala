package com.twitter.finatra.tests.utils

import com.twitter.finatra.modules.CredentialsModule
import com.twitter.finatra.tests.utils.LocalFilesystemCredentialsIntegrationTest._
import com.twitter.finatra.test.LocalFilesystemTestUtils._
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import java.io.File
import org.apache.commons.io.FileUtils

object LocalFilesystemCredentialsIntegrationTest {

  val CredentialsText =
    """
      |test_token: asdf
      |test_authorization_id: 123456
    """.stripMargin
}

class LocalFilesystemCredentialsIntegrationTest extends IntegrationTest {

  override protected def beforeAll() = {
    super.beforeAll()

    // create keys/finatra directory and add credentials.yml
    val credentialsBasePath = createFile(s"${BaseDirectory}keys/finatra")
    FileUtils.writeStringToFile(createFile(credentialsBasePath, "credentials.yml"), CredentialsText)
  }

  override protected def afterAll() = {
    // try to help clean up
    new File(s"${BaseDirectory}keys").delete
    super.afterAll()
  }

  override def injector =
    TestInjector(
      flags = Map("credentials.file.path" -> s"${BaseDirectory}keys/finatra/credentials.yml"),
      modules = Seq(CredentialsModule))

  "Credentials Module" should {

    "load credentials" in {
      val credentials = injector.instance[Credentials]
      credentials.isEmpty should be(false)

      credentials.get("test_token").get should be("asdf")
      credentials.get("test_authorization_id").get should be("123456")
    }
  }
}
