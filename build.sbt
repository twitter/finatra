import UnidocKeys._
import com.twitter.scrooge.ScroogeSBT
import ScoverageSbtPlugin.ScoverageKeys.coverageExcludedFiles
import ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages
import sbt.Keys._


lazy val buildSettings = Seq(
  version := "2.0.0.M3-SNAPSHOT",
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.6")
)

lazy val compilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
) ++ (
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) => Seq("-Ywarn-unused-import")
    case _ => Seq.empty
  }
)

val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback % "test",
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test" exclude("org.scala-lang", "scala-compiler")
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Twitter Maven" at "http://maven.twttr.com",
    "Finatra Repo" at "http://twitter.github.com/finatra",
    Resolver.sonatypeRepo("snapshots")
  ),
  compilerOptions
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := true,
  publishArtifact in (Compile, packageDoc) := true,
  publishArtifact in (Test, packageDoc) := true,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/twitter/finatra")),
  autoAPIMappings := true,
  apiURL := Some(url("https://twitter.github.io/finatra/docs/")),
  pomExtra := (
    <scm>
      <url>git://github.com/twitter/finatra.git</url>
      <connection>scm:git://github.com/twitter/finatra.git</connection>
    </scm>
    <developers>
      <developer>
        <id>twitter</id>
        <name>Twitter Inc.</name>
        <url>https://www.twitter.com/</url>
      </developer>
    </developers>
  ),
  pomPostProcess := { (node: scala.xml.Node) =>
    val rule = new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node): scala.xml.NodeSeq =
        n.nameToString(new StringBuilder()).toString match {
          case "dependency" if (n \ "groupId").text.trim == "org.scoverage" => Nil
          case _ => n
        }
    }

    new scala.xml.transform.RuleTransformer(rule).transform(node).head
  }
)

lazy val versions = new {
  val commonsCodec = "1.9"
  val commonsFileupload = "1.3.1"
  val commonsIo = "2.4"
  val finagle = "6.27.0"
  val grizzled = "1.0.2"
  val guava = "16.0.1"
  val guice = "3.0"
  val jackson = "2.4.4"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val logback = "1.0.13"
  val mustache = "0.8.12.1"
  val nscalaTime = "1.6.0"
  val servletApi = "2.5"
  val scrooge = "3.20.0"
  val slf4j = "1.7.7"
  val twitterServer = "1.12.0"
  val util = "6.26.0"
}

lazy val injectBuildSettings = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  organization := "com.twitter.inject"
)

lazy val finatraBuildSettings = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  organization := "com.twitter.finatra"
)

lazy val commonSettings = baseSettings ++ buildSettings ++ publishSettings ++ unidocSettings

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    organization := "com.twitter.finatra",
    moduleName := "finatra-root",
    unidocProjectFilter in(ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmarks)
  ).
  aggregate(
    injectCore,
    injectModules,
    injectApp,
    injectServer,
    injectRequestScope,
    injectThriftClient,
    utils,
    jackson,
    http,
    httpclient,
    slf4j,
    benchmarks,
    helloWorld,
    streamingExample,
    twitterClone,
    benchmarkServer
  )

lazy val injectCore = (project in file("inject/inject-core")).
  settings(injectBuildSettings: _*).
  settings(
    name := "inject-core",
    moduleName := "inject-core",
    coverageExcludedPackages := "net.codingwell.scalaguice.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % versions.jackson,
      "com.google.guava" % "guava" % versions.guava,
      "com.google.inject" % "guice" % versions.guice,
      "com.google.inject.extensions" % "guice-assistedinject" % versions.guice,
      "com.google.inject.extensions" % "guice-multibindings" % versions.guice,
      "com.twitter" %% "util-app" % versions.util,
      "commons-io" % "commons-io" % versions.commonsIo,
      "javax.inject" % "javax.inject" % "1",
      "joda-time" % "joda-time" % versions.jodaTime,
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.joda" % "joda-convert" % versions.jodaConvert
    )
  )

lazy val injectModules = (project in file("inject/inject-modules")).
  settings(injectBuildSettings: _*).
  settings(
    name := "inject-modules",
    moduleName := "inject-modules",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagle,
      "com.twitter" %% "util-stats" % versions.util
    )
  ).
  dependsOn(
    injectCore,
    injectCore % "test->test"
  )

lazy val injectApp = (project in file("inject/inject-app")).
  settings(injectBuildSettings: _*).
  settings(
    name := "inject-app",
    moduleName := "inject-app",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions.util
    )
  ).
  dependsOn(
    injectCore,
    injectCore % "test->test"
  )

lazy val injectServer = (project in file("inject/inject-server")).
  settings(injectBuildSettings: _*).
  settings(
    name := "inject-server",
    moduleName := "inject-server",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-stats" % versions.finagle,
      "com.twitter" %% "twitter-server" % versions.twitterServer
    )
  ).
  dependsOn(
    injectApp,
    injectApp % "test->test",
    injectModules,
    injectModules % "test->test"
  )

lazy val injectRequestScope = (project in file("inject/inject-request-scope")).
  settings(injectBuildSettings: _*).
  settings(
    name := "inject-request-scope",
    moduleName := "inject-request-scope",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagle
    )
  ).
  dependsOn(
    injectCore,
    injectCore % "test->test"
  )

lazy val injectThriftClient = (project in file("inject/inject-thrift-client")).
  settings((ScroogeSBT.newSettings ++ injectBuildSettings): _*).
  settings(
    name := "inject-thrift-client",
    moduleName := "inject-thrift-client",
    coverageExcludedPackages := "com.twitter.test.thriftscala.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thrift" % versions.finagle,
      "com.twitter" %% "finagle-thriftmux" % versions.finagle,
      "com.twitter" %% "scrooge-core" % versions.scrooge,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.finagle % "test->compile")
  ).
  dependsOn(
    injectCore,
    injectCore % "test->test",
    injectApp % "test->test",
    http % "test->test"
  )

lazy val utils = project.
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-utils",
    moduleName := "finatra-utils",
    coverageExcludedPackages := "<empty>;com\\.twitter\\.finatra\\..*package.*;.*FinatraInstalledModules.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % versions.jackson,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.google.guava" % "guava" % versions.guava,
      "com.twitter" %% "finagle-http" % versions.finagle,
      "commons-io" % "commons-io" % versions.commonsIo,
      "joda-time" % "joda-time" % versions.jodaTime,
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.joda" % "joda-convert" % versions.jodaConvert
    )
  ).
  dependsOn(
    injectRequestScope,
    injectServer,
    injectServer % "test->test"
  )

lazy val jackson = project.
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-jackson",
    moduleName := "finatra-jackson",
    coverageExcludedPackages := "scala.tools.nsc.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson exclude("org.scala-lang", "scala-compiler"),
      "org.scala-lang" % "scalap" % scalaVersion.value exclude("org.scala-lang", "scala-compiler")
    )
  ).
  dependsOn(
    injectServer % "test->test",
    utils
  )

lazy val http = project.
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-http",
    moduleName := "finatra-http",
    coverageExcludedPackages := "<empty>;.*ScalaObjectHandler.*;com\\.twitter\\.finatra\\..*package.*",
    libraryDependencies ++= Seq(
      "com.github.spullara.mustache.java" % "compiler" % versions.mustache,
      "commons-fileupload" % "commons-fileupload" % versions.commonsFileupload,
      "javax.servlet" % "servlet-api" % versions.servletApi
    ),
    unmanagedResourceDirectories in Test <+= baseDirectory(
      _ / "src" / "test" / "webapp"
    ),
    excludeFilter in Test in unmanagedResources := "BUILD"
  ).
  dependsOn(
    jackson,
    httpclient % "test->test",
    jackson % "test->test",
    injectServer % "test->test"
  )

lazy val httpclient = project.
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-httpclient",
    moduleName := "finatra-httpclient",
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % versions.commonsCodec
    )
  ).
  dependsOn(
    jackson,
    utils % "test->test",
    injectApp % "test->test"
  )

lazy val slf4j = project.
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-slf4j",
    moduleName := "finatra-slf4j",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-http" % versions.finagle,
      "org.slf4j" % "jcl-over-slf4j" % versions.slf4j,
      "org.slf4j" % "jul-to-slf4j" % versions.slf4j,
      "org.slf4j" % "log4j-over-slf4j" % versions.slf4j
    )
  ).
  dependsOn(
    http % "test->test",
    injectCore,
    injectCore % "test->test"
  )


// Can run in the SBT console in this project with `> run -wi 20 -i 10 -f 1 .*`.
lazy val benchmarks = project.
  settings((finatraBuildSettings ++ jmhSettings): _*).
  settings(
    name := "finatra-benchmarks",
    moduleName := "finatra-benchmarks",
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % "1.7.7"
    )
  ).
  dependsOn(
    http,
    injectCore % "test->test"
  )

lazy val helloWorld = (project in file("examples/finatra-hello-world")).
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-hello-world",
    moduleName := "finatra-hello-world",
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    slf4j,
    injectCore % "test->test"
  )

lazy val streamingExample = (project in file("examples/streaming-example")).
  settings(finatraBuildSettings: _*).
  settings(
    name := "streaming-example",
    moduleName := "streaming-example",
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.twitter" % "joauth" % "6.0.2"
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    slf4j,
    injectCore % "test->test"
  )

lazy val twitterClone = (project in file("examples/finatra-twitter-clone")).
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-twitter-clone",
    moduleName := "finatra-twitter-clone",
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    httpclient,
    slf4j,
    injectCore % "test->test"
  )

lazy val benchmarkServer = (project in file("examples/finatra-benchmark-server")).
  settings(finatraBuildSettings: _*).
  settings(
    name := "finatra-benchmark-server",
    moduleName := "finatra-benchmark-server",
    publishLocal := {},
    publish := {},
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % "1.7.7"
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    httpclient,
    slf4j,
    injectCore % "test->test"
  )