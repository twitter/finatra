lazy val buildSettings = Seq(
  version := "2.0.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.6")
)

lazy val compilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
) ++ (
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) => Seq("-Ywarn-unused-import")
    case _ => Seq.empty
  }
  )

val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13" % "test",
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
  ),
  resolvers ++= Seq(
    "twitter-repo" at "http://maven.twttr.com",
    Resolver.sonatypeRepo("snapshots")
  ),
  compilerOptions
)

/**
 * Can run in the SBT console in this project with `> run`.
 */
lazy val finatraStandaloneHelloWorld = project
  .in(file("."))
  .settings(organization := "com.twitter.example")
  .settings(moduleName := "finatra-hello-world")
  .settings(baseSettings ++ buildSettings)
  .settings(
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    }
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter.finatra" %% "finatra-http" % "2.0.0.rc1",
      "com.twitter.finatra" %% "finatra-http" % "2.0.0.rc1" % "test->test",
      "com.twitter.inject" %% "inject-server" % "2.0.0.rc1" % "test->test"
    )
  )