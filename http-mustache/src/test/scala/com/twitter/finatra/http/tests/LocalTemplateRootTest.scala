package com.twitter.finatra.http.tests

import com.twitter.finatra.test.LocalFilesystemTestUtils.{createFile, writeStringToFile}

/** Tests when the mustache template directory is different from the local doc root directory */
class LocalTemplateRootTest extends AbstractTemplateRootTest {

  def templateRootDirectory(baseFolderName: String): String =
    s"${baseFolderName}src/main/resources/templates"

  /* same as the root */
  def fullTemplatePath(baseFolderName: String): String = templateRootDirectory(baseFolderName)

  def setup(baseFolderName: String): Unit = {
    val templateRootDirectory: String = s"${baseFolderName}src/main/resources/templates"

    // create src/main/resources/templates directory and add files
    val templates = createFile(templateRootDirectory)
    writeStringToFile(createFile(templates, "testuser.mustache"), testUserMustacheString)
    writeStringToFile(
      createFile(templates, "testuser2.mustache"),
      testUser2MustacheString
    )
    writeStringToFile(createFile(templates, "testHtml.mustache"), testHtmlMustacheString)
  }
}
