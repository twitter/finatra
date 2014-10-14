name := "finatra"

organization := "com.twitter"

version := "1.5.4"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.9.2", "2.10.3")

//Main

libraryDependencies ++= Seq(
  "com.twitter" %% "twitter-server" % "1.7.1",
  "commons-io" % "commons-io" % "1.3.2",
  "org.scalatest" %% "scalatest" % "1.9.2",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.2",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.14",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
)

// for code coverage
instrumentSettings 

coverallsSettings

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
