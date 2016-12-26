resolvers ++= Seq(
  Classpaths.sbtPluginSnapshots,
  Classpaths.sbtPluginReleases,
  Resolver.sonatypeRepo("snapshots")
)

val branch = Process("git" :: "rev-parse" :: "--abbrev-ref" :: "HEAD" :: Nil).!!.trim
val scroogeSbtPluginVersionPrefix = "4.13.0"
val scroogeSbtPluginVersion =
  if (branch == "master") scroogeSbtPluginVersionPrefix
  else scroogeSbtPluginVersionPrefix + "-SNAPSHOT"
addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % scroogeSbtPluginVersion)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.16")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
