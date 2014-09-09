import FinatraDependencies._
import sbt._

name := "finatra"

organization := "com.twitter"

version := "2.0.0"

scalaVersion in ThisBuild := Version_2_9

crossScalaVersions := Seq(Version_2_9, Version_2_10)

////////////////
/// Projects ///
////////////////

lazy val finatra = project in file(".") aggregate(
  finatraBenchmarks,
  finatraHelloWorld,
  finatraHttpClient,
  finatraJackson,
  finatraLogback,
  finatraScopes,
  finatraServer,
  finatraSwagger,
  finatraTwitterExample,
  finatraUtils)

lazy val finatraUtils = project.in(file("finatra/finatra-utils"))
  .settings(
    libraryDependencies <+= scalaVersion(scalaj_time),
    libraryDependencies ++= Seq(
      commons_io,
      finagle_core,
      finagle_http,
      guava,
      guice_assistedinject,
      libthrift,
      netty,
      scala_guice,
      util_logging,
      util_core))

lazy val finatraJackson = project.in(file("finatra/finatra-jackson"))
  .dependsOn(finatraUtils)
  .settings(
    libraryDependencies <+= scalaVersion(scalaj_time),
    libraryDependencies <+= scalaVersion(scalap),
    libraryDependencies ++= Seq(
      commons_lang,
      jackson_annotations,
      jackson_core,
      jackson_databind,
      jackson_datatype_joda,
      jackson_module_scala,
      joda_time,
      guice,
      scala_guice))

lazy val finatraServer = project.in(file("finatra/finatra-server")).
  dependsOn(finatraUtils % "compile->compile; test->test").
  dependsOn(finatraJackson % "compile->compile; test->test").
  settings(
    parallelExecution in Test := false,
    unmanagedResourceDirectories in Test += baseDirectory.value / "src" / "test" / "webapp",
    libraryDependencies ++= Seq(
      commons_fileupload,
      commons_io,
      guava,
      guice,
      finagle_core,
      finagle_http,
      javax_inject,
      javax_servlet_api,
      mustache_compiler,
      netty,
      scala_guice,
      twitter_server,
      util_app,
      util_logging))

lazy val finatraLogback = project.in(file("finatra/finatra-logback")).
  dependsOn(finatraServer % "compile->compile; test->test").
  dependsOn(finatraUtils % "compile->compile; test->test").
  settings(
    libraryDependencies ++= Seq(
      jcl_over_slf4j,
      jul_over_slf4j,
      log4j_over_slf4j,
      logback_classic,
      util_core,
      util_logging))

lazy val finatraHttpClient = project.in(file("finatra/finatra-httpclient")).
  dependsOn(finatraJackson).
  dependsOn(finatraUtils % "compile->compile; test->test")

lazy val finatraBenchmarks = project.in(file("finatra/finatra-benchmarks")).
  dependsOn(finatraServer).
  settings(
    libraryDependencies ++= Seq(
      jmh_core))

lazy val finatraScopes = project.in(file("finatra/finatra-scopes")).
  dependsOn(finatraServer).
  settings(
    libraryDependencies ++= Seq(
      commons_codec,
      finagle_core,
      finagle_http,
      findbugs,
      guice,
      javax_inject,
      scala_guice,
      util_core))

lazy val finatraSwagger = project.in(file("finatra-experimental/finatra-swagger")).
  dependsOn(finatraJackson % "compile->compile; test->test").
  dependsOn(finatraScopes).
  dependsOn(finatraServer % "compile->compile; test->test").
  dependsOn(finatraUtils % "compile->compile; test->test").
  settings(
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src" / "main" / "webapp",
    libraryDependencies ++= Seq(
      swagger_annotations))

lazy val finatraHelloWorld = project.in(file("finatra/finatra-examples/finatra-hello-world")).
  dependsOn(finatraLogback).
  dependsOn(finatraServer % "test->test")

lazy val finatraTwitterExample = project.in(file("finatra/finatra-examples/finatra-twitter-example")).
  dependsOn(finatraHttpClient).
  dependsOn(finatraLogback).
  dependsOn(finatraScopes).
  dependsOn(finatraServer % "compile->compile; test->test").
  dependsOn(finatraUtils % "compile->compile; test->test")
