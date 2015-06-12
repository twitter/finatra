val finatraVersion = "2.0.0.M2"

lazy val finatraHelloWorld = project
  .in(file("."))
  .settings(
    name := "finatra-hello-world",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    organization := "com.twitter.example",
    moduleName := "finatra-hello-world",
    resolvers += "twitter-repo" at "http://maven.twttr.com",
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", "-deprecation", "-feature", "-unchecked",
      "-Ywarn-dead-code", "-Ywarn-numeric-widen", "-Ywarn-unused-import",
      "-language:existentials", "-language:higherKinds", "-language:implicitConversions"),
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "com.twitter.finatra" %% "finatra-http" % finatraVersion,
      "com.twitter.finatra" %% "finatra-logback" % finatraVersion,

      "com.twitter.finatra" %% "finatra-http" % finatraVersion % "test",
      "com.twitter.inject" %% "inject-server" % finatraVersion % "test",
      "com.twitter.inject" %% "inject-app" % finatraVersion % "test",
      "com.twitter.inject" %% "inject-core" % finatraVersion % "test",
      "com.twitter.inject" %% "inject-modules" % finatraVersion % "test",
      "com.twitter.finatra" %% "finatra-http" % finatraVersion % "test" classifier "tests",
      "com.twitter.inject" %% "inject-server" % finatraVersion % "test" classifier "tests",
      "com.twitter.inject" %% "inject-app" % finatraVersion % "test" classifier "tests",
      "com.twitter.inject" %% "inject-core" % finatraVersion % "test" classifier "tests",
      "com.twitter.inject" %% "inject-modules" % finatraVersion % "test" classifier "tests",

      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.scalatest" %% "scalatest" % "2.2.3" % "test",
      "org.specs2" %% "specs2" % "2.3.12" % "test"))
