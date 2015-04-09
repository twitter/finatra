resolvers ++= Seq(
  Classpaths.sbtPluginSnapshots,
  Resolver.sonatypeRepo("snapshots")
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.5-SNAPSHOT")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.8")
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.15.0")
