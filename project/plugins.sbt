resolvers ++= Seq(
  Classpaths.sbtPluginSnapshots,
  Classpaths.sbtPluginReleases,
  Resolver.sonatypeRepo("snapshots"),
  "Twitter Maven" at "https://maven.twttr.com"
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.8")
addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "4.5.0")
// sbt-scoverage 1.3.3 and 1.3.5 have bugs that result in 2.10 tests not being run.
// See https://github.com/scoverage/sbt-scoverage/issues/146
// and https://github.com/scoverage/sbt-scoverage/issues/161
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
