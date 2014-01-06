name := "###PROJECT_NAME###"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.twitter" %% "finatra" % "###VERSION###"
)

resolvers +=
  "Twitter" at "http://maven.twttr.com"
