package com.twitter.finatra.tests.utils

import com.twitter.finatra.modules.{FileResolverFlags, FileResolverModule}
import com.twitter.finatra.test.LocalFilesystemTestUtils.{createFile, writeStringToFile}
import com.twitter.finatra.utils.AutoClosable.tryWith
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.{Injector, Test}
import com.twitter.inject.app.TestInjector
import com.twitter.io.{StreamIO, TempFolder}
import java.io.InputStream

class LocalDocumentRootIntegrationTest extends Test with TempFolder {
  private val testFileText: String = "testfile123"
  private val testIndexHtml: String = "testindex"

  test("LocalDocumentRoot#load files") {
    withTempFolder {
      setup(folderName)

      val injector: Injector = TestInjector(
        modules = Seq(FileResolverModule),
        flags = Map[String, String](FileResolverFlags.LocalDocRoot -> documentRoot(folderName))
      ).create

      val fileResolver = injector.instance[FileResolver]
      fileResolver.getInputStream("/testfile.txt").map(body) should be(Some(testFileText))
      fileResolver.getInputStream("/testindex.html").map(body) should be(Some(testIndexHtml))
      fileResolver.getInputStream("/doesntexist.txt").map(body) should be(None)
    }
  }

  private def setup(folderName: String): Unit = {
    // create src/main/webapp directory and add files
    val webapp = createFile(documentRoot(folderName))
    writeStringToFile(createFile(webapp, "testfile.txt"), testFileText)
    writeStringToFile(createFile(webapp, "testindex.html"), testIndexHtml)
  }

  private def documentRoot(folderName: String): String = s"${folderName}src/main/webapp"

  private[this] def body(inputStream: InputStream): String = {
    tryWith(inputStream) { closable =>
      StreamIO.buffer(closable).toString
    }
  }
}
