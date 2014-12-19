name := "finatra"

organization := "com.twitter"

version := "1.5.4-SNAPSHOT"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.10.4", "2.11.4")

//Main

libraryDependencies ++= Seq(
  "com.twitter" %% "twitter-server" % "1.9.0",
  "commons-io" % "commons-io" % "1.3.2",
  "org.scalatest" %% "scalatest" % "2.2.1",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.2",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.17",
  "com.github.spullara.mustache.java" % "scala-extensions-2.10" % "0.8.17",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

//Release

resolvers +=
  "Twitter" at "http://maven.twttr.com"

resolvers +=
  "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

resolvers += Classpaths.sbtPluginReleases

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("Apache License, Version 2.0" ->
  url("http://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("http://finatra.info"))

pomExtra := (
  <developers>
    <developer>
      <id>julio</id>
      <name>Julio Capote</name>
      <email>julio@twitter.com</email>
    </developer>
    <developer>
      <id>chris</id>
      <name>Christopher Burnett</name>
      <email>cburnett@twitter.com</email>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git@github.com:twitter/finatra.git</connection>
    <url>scm:git:git@github.com:twitter/finatra.git</url>
    <developerConnection>scm:git:git@github.com:twitter/finatra.git</developerConnection>
  </scm>)
