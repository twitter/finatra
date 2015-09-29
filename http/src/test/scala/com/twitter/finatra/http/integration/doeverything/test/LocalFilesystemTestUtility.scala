package com.twitter.finatra.http.integration.doeverything.test

import java.io.File

trait LocalFilesystemTestUtility {
  val baseDirectory = addSlash(System.getProperty("java.io.tmpdir"))

  val testUserMustacheString =
    "age:{{age}}\nname:{{name}}\n{{#friends}}\n{{.}}\n{{/friends}}"
  val testUser2MustacheString =
    "age2:{{age}}\nname2:{{name}}\n{{#friends}}\n{{.}}\n{{/friends}}"
  val testHtmlMustacheString =
    """<div class="nav">
      |  <table cellpadding="0" cellspacing="0">
      |    <tr>
      |        <th>Name</th>
      |        <th>Age</th>
      |        <th>Friends</th>
      |    </tr>
      |    <tr>
      |        <td>age2:{{age}}</td>
      |        <td>name:{{name}}</td>
      |        <td>
      |            {{#friends}}
      |            {{.}}
      |            {{/friends}}
      |        </td>
      |    </tr>
      |  </table>
      |</div>""".stripMargin

  val testFileText = "testfile123"
  val testIndexHtml = "testindex"

  def createFile(path: String): File = {
    val f = new File(path); f.deleteOnExit(); f
  }

  def createFile(parent: File, path: String): File = {
    val f = new File(parent, path); f.deleteOnExit(); f
  }

  def addSlash(directory: String): String = {
    if (directory.endsWith("/")) directory else s"$directory/"
  }
}
