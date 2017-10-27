package com.twitter.inject.server.tests

import com.google.inject.name.Names
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}
import com.twitter.inject.{Logging, Test, TwitterModule}
import com.twitter.util.{Future, Await}
import com.twitter.util.lint.{Rules, RulesImpl, GlobalRules, Rule, Issue, Category}
import com.twitter.util.registry.{GlobalRegistry, SimpleRegistry}
import org.apache.commons.lang.RandomStringUtils

class EmbeddedTwitterServerIntegrationTest extends Test {

  private [this] def generateTestRuleName: String = {
    s"TestRule-${RandomStringUtils.randomAlphabetic(12).toUpperCase()}"
  }

  private def mkRules(rules: Rule*): Rules = {
    val toReturn = new RulesImpl()
    rules.foreach(rule => toReturn.add(rule))
    toReturn
  }

  private val alwaysRule1 =
    Rule(
      Category.Configuration,
      generateTestRuleName,
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit.") {
      Seq(
        Issue("Etiam nisi metus, commodo in erat id, sagittis luctus purus."),
        Issue("Vestibulum sagittis justo a ex suscipit, sit amet efficitur mi varius."),
        Issue("Maecenas eu condimentum nulla, non porta tortor."))
    }
  private val alwaysRule2 =
    Rule(
      Category.Configuration,
      generateTestRuleName,
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla aliquam eu ante et auctor. " +
        "Vestibulum sagittis justo a ex suscipit, sit amet efficitur mi varius. Maecenas egestas " +
        "viverra arcu, id volutpat magna molestie sit amet.") {
      Seq(Issue("Duis blandit orci mi, sit amet euismod magna maximus eu."))
    }
  private val alwaysRule3 =
    Rule(
      Category.Configuration,
      generateTestRuleName,
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla aliquam eu ante et auctor. " +
        "Vestibulum sagittis justo a ex suscipit, sit amet efficitur mi varius. Maecenas egestas " +
        "viverra arcu, id volutpat magna molestie sit amet.") {
      Seq(Issue("This is just a test."))
    }
  private val neverRule =
    Rule(
      Category.Configuration,
      generateTestRuleName,
      "Donec ligula nibh, accumsan a tempor a, consequat sit amet enim.") {
      Seq.empty
    }

  test("server#start") {
    val twitterServer = new TwitterServer {}
    twitterServer.addFrameworkOverrideModules(new TwitterModule {})
    val embeddedServer = new EmbeddedTwitterServer(twitterServer)

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")
    } finally {
      embeddedServer.close()
    }
  }

  test("server#fail if server is a singleton") {
    intercept[IllegalArgumentException] {
      new EmbeddedTwitterServer(SingletonServer)
    }
  }

  test("server#fail if bind on a non-injectable server") {
    intercept[IllegalStateException] {
      new EmbeddedTwitterServer(new NonInjectableServer)
        .bind[String]("hello!")
    }
  }

  test("server#support bind in server") {
    val server =
      new EmbeddedTwitterServer(new TwitterServer {})
        .bind[String]("helloworld")

    try {
      server.injector.instance[String] should be("helloworld")
    } finally {
      server.close()
    }
  }

  test("server#support bind with @Named in server") {
    val server =
      new EmbeddedTwitterServer(new TwitterServer {})
        .bind[String](Names.named("best"), "helloworld")

    try {
      server.injector.instance[String]("best") should be("helloworld")
    } finally {
      server.close()
    }
  }

  test("server#fail because of unknown flag") {
    val server = new EmbeddedTwitterServer(new TwitterServer {}, flags = Map("foo.bar" -> "true"))

    try {
      val e = intercept[Exception] {
        server.assertHealthy()
      }
      e.getMessage.contains("Error parsing flag \"foo.bar\": flag undefined") should be(true)
    } finally {
      server.close()
    }
  }

  // Currently fails in sbt
  ignore("server#fail startup because of linting violations") {
    val rules = mkRules(alwaysRule1, alwaysRule2, neverRule)

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new TwitterServer {}, failOnLintViolation = true)
        try {
          val e = intercept[Exception] {
            server.assertHealthy()
          }
          e.getMessage.contains("and 4 Linter issues found") should be(true)
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#starts when there is an artificial rule but no violations and failOnLintViolation = true") {
    val rules = mkRules(neverRule)

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new TwitterServer {}, failOnLintViolation = true)

        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#starts when there are no lint rule violations and failOnLintViolation = true") {
    val rules = mkRules()

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new TwitterServer {}, failOnLintViolation = true)

        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#starts when there are linting violations and failOnLintViolation = false") {
    val rules = mkRules(alwaysRule1, alwaysRule2, alwaysRule3)

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new TwitterServer {})

        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#non-injectable server fail startup because linting violation and failOnLintViolation = true") {
    val rules = mkRules(alwaysRule3)

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new NonInjectableServer, failOnLintViolation = true)
        try {
          val e = intercept[Exception] {
            server.assertHealthy()
          }
          e.getMessage.contains("and 1 Linter issue found") should be(true)
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#non-injectable server starts when there are linting violations and failOnLintViolation = false") {
    val rules = mkRules(alwaysRule3)

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new NonInjectableServer)
        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#non-injectable server starts when there are no linting violations and and failOnLintViolation = false") {
    val rules = mkRules()

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new NonInjectableServer)
        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }

  // Currently fails in sbt
  ignore("server#non-injectable server starts when there are no linting violations and and failOnLintViolation = true") {
    val rules = mkRules()

    GlobalRegistry.withRegistry(new SimpleRegistry) {
      GlobalRules.withRules(rules) {
        val server = new EmbeddedTwitterServer(new NonInjectableServer, failOnLintViolation = true)
        try {
          server.assertHealthy()
        } finally {
          server.close()
        }
      }
    }
  }
}

class NonInjectableServer extends com.twitter.server.TwitterServer with Logging {
  val service = new Service[Request, Response] {
    def apply(request: Request) = {
      val response = Response(request.version, Status.Ok)
      response.contentString = "hello"
      Future.value(response)
    }
  }

  def main() {
    val server = Http.serve(":8888", service)
    onExit {
      server.close()
    }
    Await.ready(server)
  }
}

object SingletonServer extends TwitterServer
