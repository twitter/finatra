package com.twitter.finatra.http.tests

trait FileUtility {

  protected val testUserMustacheString: String =
    "age:{{age}}\nname:{{name}}\n{{#friends}}\n{{.}}\n{{/friends}}"
  protected val testUser2MustacheString: String =
    "age2:{{age}}\nname2:{{name}}\n{{#friends}}\n{{.}}\n{{/friends}}"
  protected val testHtmlMustacheString: String =
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

}
