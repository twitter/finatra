name := "finatra"

version := "1.4.2-SNAPSHOT"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.9.2", "2.10.3")

libraryDependencies ++= Seq(
  "com.twitter" % "twitter-server_2.10" % "1.1.0",
  "commons-io" % "commons-io" % "1.3.2",
  "org.scalatest" % "scalatest_2.10.0" % "2.0.M5",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.google.guava" % "guava" % "15.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.2.2",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.13",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
)

resolvers +=
  "Twitter" at "http://maven.twttr.com"
