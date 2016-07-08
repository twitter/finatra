import sbt.Keys._

parallelExecution in ThisBuild := false

lazy val versions = new {
  val finatra = "2.2.0"
  val guice = "4.0"
  val logback = "1.1.7"
}

lazy val baseSettings = Seq(
  version := "2.2.0",
  scalaVersion := "2.11.8",
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Twitter Maven" at "https://maven.twttr.com"
  ),
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val root = (project in file(".")).
  settings(
    organization := "com.twitter",
    moduleName := "thrift-example-root"
  ).
  aggregate(
    thriftExampleIdl,
    thriftExampleServer
  )

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
  dependsOn(
    thriftExampleIdl
  )
