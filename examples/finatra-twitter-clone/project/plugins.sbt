resolvers ++= Seq(
  Classpaths.sbtPluginSnapshots
)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.1.0")
