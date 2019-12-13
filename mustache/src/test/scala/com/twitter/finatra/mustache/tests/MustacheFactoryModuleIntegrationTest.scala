package com.twitter.finatra.mustache.tests

import com.github.mustachejava.MustacheNotFoundException
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.finatra.mustache.marshalling.MustacheService
import com.twitter.finatra.mustache.modules.MustacheFactoryModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}

case class TestHtml(name: String, age: Int, friends: Seq[String])
case class TestJson(name: String)
case class TestUser(name: String, age: Int, friends: Seq[String])
case class TestUser2(name: String, age: Int, friends: Seq[String])

class MustacheFactoryModuleIntegrationTest extends IntegrationTest {
  override val injector: Injector =
    TestInjector(FileResolverModule, MustacheFactoryModule).create
  private val mustacheService: MustacheService = injector.instance[MustacheService]

  test("template not found") {
    intercept[MustacheNotFoundException] {
      mustacheService.createString("doesnotexist.mustache", Seq.empty[String])
    }
  }

  test("testHtml.mustache") {
    val expected =
      """<div class="nav">
        |  <table cellpadding="0" cellspacing="0">
        |    <tr>
        |        <th>Name</th>
        |        <th>Age</th>
        |        <th>Friends</th>
        |    </tr>
        |    <tr>
        |        <td>age2:45</td>
        |        <td>name:Bob</td>
        |        <td>
        |            Alice
        |        </td>
        |    </tr>
        |  </table>
        |</div>""".stripMargin

    val result =
      mustacheService.createString("testHtml.mustache", TestHtml("Bob", 45, Seq("Alice")))
    result should equal(expected)
  }

  test("testJson.mustache") {
    val expected =
      """{
        |  "name": "Bob"
        |}""".stripMargin

    val result = mustacheService.createString("testJson.mustache", TestJson("Bob"))
    result should equal(expected)
  }

  test("testuser.mustache") {
    val expected =
      """age:45
        |name:Bob
        |Alice
        |""".stripMargin

    val result =
      mustacheService.createString("testuser.mustache", TestUser("Bob", 45, Seq("Alice")))
    result should equal(expected)
  }

  test("testuser2.mustache") {
    val expected =
      """age2:45
        |name2:Bob
        |Alice
        |""".stripMargin

    val result =
      mustacheService.createString("testuser2.mustache", TestUser2("Bob", 45, Seq("Alice")))
    result should equal(expected)
  }

}
