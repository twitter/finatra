package com.twitter.finatra.http.tests

import com.twitter.finatra.test.LocalFilesystemTestUtils.{createFile, writeStringToFile}

/** Tests when the mustache template directory is relative to local doc root directory */
class OverlappingTemplateRootTest extends AbstractTemplateRootTest {

  def templateRootDirectory(baseFolderName: String): String = "/templates"

  /* different from the specified root as the full path is relative to the webapp directory */
  def fullTemplatePath(baseFolderName: String): String = s"${baseFolderName}src/main/webapp/templates"

  def setup(baseFolderName: String): Unit = {
    // create src/main/webapp directory and add files
    val webapp = createFile(s"${baseFolderName}src/main/webapp")

    // create /templates directory *under* webapp and add files
    val templates = createFile(webapp, "templates")
    writeStringToFile(createFile(templates, "testuser.mustache"), testUserMustacheString)
    writeStringToFile(
      createFile(templates, "testuser2.mustache"),
      testUser2MustacheString
    )
    writeStringToFile(createFile(templates, "testHtml.mustache"), testHtmlMustacheString)
  }
}
