import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

packageArchetype.java_application

bashScriptConfigLocation := Some("${app_home}/../conf/application.ini")

lazy val finatraHelloWorldHeroku = project
  .in(file("."))
  .settings(
    name := "finatra-hello-world",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    organization := "com.twitter.example",
    moduleName := "finatra-hello-world",
    fork in run := true,
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
      "com.twitter.finatra" %% "finatra-http" % "2.0.0.M1",
      "com.twitter.finatra" %% "finatra-logback" % "2.0.0.M1",

      "com.twitter.finatra" %% "finatra-http" % "2.0.0.M1" % "test",
      "com.twitter.inject" %% "inject-server" % "2.0.0.M1" % "test",
      "com.twitter.inject" %% "inject-app" % "2.0.0.M1" % "test",
      "com.twitter.inject" %% "inject-core" % "2.0.0.M1" % "test",
      "com.twitter.inject" %% "inject-modules" % "2.0.0.M1" % "test",
      "com.twitter.finatra" %% "finatra-http" % "2.0.0.M1" % "test" classifier "tests",
      "com.twitter.inject" %% "inject-server" % "2.0.0.M1" % "test" classifier "tests",
      "com.twitter.inject" %% "inject-app" % "2.0.0.M1" % "test" classifier "tests",
      "com.twitter.inject" %% "inject-core" % "2.0.0.M1" % "test" classifier "tests",
      "com.twitter.inject" %% "inject-modules" % "2.0.0.M1" % "test" classifier "tests",

      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.scalatest" %% "scalatest" % "2.2.3" % "test",
      "org.specs2" %% "specs2" % "2.3.12" % "test"))
