resolvers ++= Seq(
  Classpaths.sbtPluginSnapshots,
  Classpaths.sbtPluginReleases,
  Resolver.sonatypeRepo("snapshots") ,
  Resolver.url("scoverage-bintray",
    url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.8")
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.15.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0")
