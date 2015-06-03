name := "finatra-seed"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.6"

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
  "org.specs2" %% "specs2" % "2.3.12" % "test")

fork in run := true