package com.twitter.finatra.http.integration.doeverything.test

trait DocRootLocalFilesystemTestUtility {
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
}
