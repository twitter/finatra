name := "finatra"

version := "1.4.2-SNAPSHOT"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.9.2", "2.10.0")

libraryDependencies ++= Seq(
  "com.twitter" %% "twitter-server" % "1.1.0",
  "commons-io" % "commons-io" % "1.3.2",
  "org.scalatest" %% "scalatest" % "1.9.2",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.google.guava" % "guava" % "15.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.2",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.13",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
)

resolvers +=
  "Twitter" at "http://maven.twttr.com"
