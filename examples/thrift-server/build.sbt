import sbt.Keys._

parallelExecution in ThisBuild := false

lazy val versions = new {
  val finatra = "2.4.0"
  val guice = "4.0"
  val logback = "1.1.7"
  val mockito = "1.9.5"
  val scalatest = "2.2.6"
  val specs2 = "2.3.12"
}

lazy val baseSettings = Seq(
  version := "2.4.0",
  scalaVersion := "2.11.8",
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "org.scalatest" %% "scalatest" % versions.scalatest % "test",
    "org.specs2" %% "specs2" % versions.specs2 % "test"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Twitter Maven" at "https://maven.twttr.com"
  ),
  fork in run := true,
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val root = (project in file(".")).
  settings(
    name := "thrift-server",
    organization := "com.twitter",
    moduleName := "thrift-example-root",
    run := {
      (run in `thriftExampleServer` in Compile).evaluated
    }
  ).
  aggregate(thriftExampleServer)

lazy val thriftExampleServer = (project in file("thrift-example-server")).
  settings(baseSettings).
  settings(
    name := "thrift-example-server",
    moduleName := "thrift-example-server",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra-thrift" % versions.finatra,
      "ch.qos.logback" % "logback-classic" % versions.logback,

      "com.twitter" %% "finatra-thrift" % versions.finatra % "test",
      "com.twitter" %% "inject-app" % versions.finatra % "test",
      "com.twitter" %% "inject-core" % versions.finatra % "test",
      "com.twitter" %% "inject-modules" % versions.finatra % "test",
      "com.twitter" %% "inject-server" % versions.finatra % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",

      "com.twitter" %% "finatra-thrift" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests"
    )
  ).
  dependsOn(thriftExampleIdl)

lazy val thriftExampleIdl = (project in file("thrift-example-idl")).
    settings(baseSettings).
    settings(
      name := "thrift-example-idl",
      moduleName := "thrift-example-idl",
      scroogeThriftDependencies in Compile := Seq(
        "finatra-thrift_2.11"
      ),
      libraryDependencies ++= Seq(
        "com.twitter" %% "finatra-thrift" % versions.finatra
      )
    )
