resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Resolver.sonatypeRepo("snapshots")
)

val releaseVersion = "21.7.0-SNAPSHOT"

addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % releaseVersion)

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
