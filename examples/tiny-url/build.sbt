import com.typesafe.sbt.SbtNativePackager._

packageArchetype.java_application
name := "tiny-url"
organization := "com.twitter"
version := "2.10.0"
scalaVersion := "2.11.8"
fork in run := true
parallelExecution in ThisBuild := false

lazy val versions = new {
  val finatra = "2.10.0"
  val guice = "4.0"
  val logback = "1.1.7"
  val redis = "2.7.2"
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Twitter Maven" at "https://maven.twttr.com"
)

assemblyMergeStrategy in assembly := {
  case "BUILD" => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  case other => MergeStrategy.defaultMergeStrategy(other)
}

libraryDependencies ++= Seq(
  "com.twitter" %% "finatra-http" % versions.finatra,
  "com.twitter" %% "finatra-httpclient" % versions.finatra,
  "ch.qos.logback" % "logback-classic" % versions.logback,
  "redis.clients" % "jedis" % versions.redis,

  "com.twitter" %% "finatra-http" % versions.finatra % "test",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test",
  "com.twitter" %% "inject-server" % versions.finatra % "test",
  "com.twitter" %% "inject-app" % versions.finatra % "test",
  "com.twitter" %% "inject-core" % versions.finatra % "test",
  "com.twitter" %% "inject-modules" % versions.finatra % "test",
  "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",

  "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",

  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalatest" %% "scalatest" %  "3.0.0" % "test",
  "org.specs2" %% "specs2-mock" % "2.4.17" % "test")

resourceGenerators in Compile += Def.task {
  val dir = (resourceManaged in Compile).value
  val file = dir / "build.properties"
  val buildRev = Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
  val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
  val contents = "name=%s\nversion=%s\nbuild_revision=%s\nbuild_name=%s".format(name.value, version.value, buildRev, buildName)
  IO.write(file, contents)
  Seq(file)
}.taskValue
