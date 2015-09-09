import com.typesafe.sbt.SbtNativePackager._

packageArchetype.java_application

name := "tiny-url"
organization := "com.twitter.tiny"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.6"

fork in run := true

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Twitter Maven" at "http://maven.twttr.com",
  "Finatra Repo" at "http://twitter.github.com/finatra"
)

libraryDependencies ++= Seq(
  "redis.clients" % "jedis" % "2.7.2",

  "com.twitter.finatra" %% "finatra-http" % "2.0.0.M2",
  "com.twitter.finatra" %% "finatra-logback" % "2.0.0.M2",

  "com.twitter.finatra" %% "finatra-http" % "2.0.0.M2" % "test",
  "com.twitter.finatra" %% "finatra-jackson" % "2.0.0.M2" % "test",
  "com.twitter.inject" %% "inject-server" % "2.0.0.M2" % "test",
  "com.twitter.inject" %% "inject-app" % "2.0.0.M2" % "test",
  "com.twitter.inject" %% "inject-core" % "2.0.0.M2" % "test",
  "com.twitter.inject" %% "inject-modules" % "2.0.0.M2" % "test",
  "com.twitter.finatra" %% "finatra-http" % "2.0.0.M2" % "test" classifier "tests",
  "com.twitter.finatra" %% "finatra-jackson" % "2.0.0.M2" % "test" classifier "tests",
  "com.twitter.inject" %% "inject-server" % "2.0.0.M2" % "test" classifier "tests",
  "com.twitter.inject" %% "inject-app" % "2.0.0.M2" % "test" classifier "tests",
  "com.twitter.inject" %% "inject-core" % "2.0.0.M2" % "test" classifier "tests",
  "com.twitter.inject" %% "inject-modules" % "2.0.0.M2" % "test" classifier "tests",

  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.specs2" %% "specs2" % "2.3.12" % "test")

resourceGenerators in Compile <+=
  (resourceManaged in Compile, name, version) map { (dir, name, ver) =>
    val file = dir / "build.properties"
    val buildRev = Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
    val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
    val contents = (
      "name=%s\nversion=%s\nbuild_revision=%s\nbuild_name=%s"
    ).format(name, ver, buildRev, buildName)
    IO.write(file, contents)
    Seq(file)
  }
