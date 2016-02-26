import sbt.Keys._
import sbtunidoc.Plugin.UnidocKeys._
import scala.language.reflectiveCalls
import ScoverageSbtPlugin.ScoverageKeys._

parallelExecution in ThisBuild := false

lazy val aggregated = taskKey[Unit]("Print currently aggregated tasks under the root.")
lazy val projectVersion = "2.1.5-SNAPSHOT"

lazy val buildSettings = Seq(
  version := projectVersion,
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7"),
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
  fork in Test := true
)

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")
  val branch = Process("git" :: "rev-parse" :: "--abbrev-ref" :: "HEAD" :: Nil).!!.trim
  val suffix = if (branch == "master" || travisBranch == "master") "" else "-SNAPSHOT"

  // Use SNAPSHOT versions of Twitter libraries on non-master branches
  val finagle = "6.33.0" + suffix
  val scrooge = "4.5.0" + suffix
  val twitterServer = "1.18.0" + suffix
  val util = "6.32.0" + suffix

  val commonsCodec = "1.9"
  val commonsFileupload = "1.3.1"
  val commonsIo = "2.4"
  val commonsLang = "2.6"
  val grizzled = "1.0.2"
  val guava = "16.0.1"
  val guice = "4.0"
  val scalaGuice = "4.0.0"
  val jackson = "2.4.4"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val logback = "1.0.13"
  val mustache = "0.8.18"
  val nscalaTime = "1.6.0"
  val servletApi = "2.5"
  val snakeyaml = "1.12"
  val slf4j = "1.7.7"
  val libThrift = "0.5.0-1"
}

lazy val scalaCompilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint"
) ++ (
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 11 => Seq("-Ywarn-unused-import")
    case _ => Seq.empty
  }
)

lazy val javaCompilerOptions = javacOptions ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:unchecked"
)

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Twitter Maven" at "https://maven.twttr.com",
    Resolver.sonatypeRepo("snapshots")
  ),
  scalaCompilerOptions,
  javaCompilerOptions
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
  pomExtra :=
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
    </developers>,
  pomPostProcess := { (node: scala.xml.Node) =>
    val rule = new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node): scala.xml.NodeSeq =
        n.nameToString(new StringBuilder()).toString() match {
          case "dependency" if (n \ "groupId").text.trim == "org.scoverage" => Nil
          case _ => n
        }
    }

    new scala.xml.transform.RuleTransformer(rule).transform(node).head
  }
)

lazy val slf4jSimpleTestDependency = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % versions.slf4j % "test"
  )
)

lazy val injectBuildSettings = baseSettings ++ buildSettings ++ publishSettings ++ slf4jSimpleTestDependency ++ Seq(
  organization := "com.twitter.inject"
)

lazy val finatraBuildSettings = baseSettings ++ buildSettings ++ publishSettings ++ slf4jSimpleTestDependency ++ Seq(
  organization := "com.twitter.finatra"
)

lazy val baseServerBuildSettings = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  publishLocal := {},
  publish := {},
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val exampleServerBuildSettings = baseServerBuildSettings ++ Seq(
  organization := "com.twitter.finatra.example",
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback)
)

lazy val finatraModules = Seq(
  benchmarks,
  http,
  httpclient,
  injectApp,
  injectCore,
  injectModules,
  injectRequestScope,
  injectServer,
  injectThriftClient,
  injectThriftClientHttpMapper,
  injectUtils,
  jackson,
  slf4j,
  thrift,
  utils)

lazy val finatraExamples =
  // START EXAMPLES
  Seq(
    benchmarkServer,
    exampleInjectJavaServer,
    helloWorld,
    //helloWorldHeroku, // 2.11 only
    streamingExample,
    thriftExampleIdl,
    thriftExampleServer,
    tinyUrl,
    twitterClone) ++
  // END EXAMPLES
  Seq.empty

def aggregatedProjects = {
  if (projectVersion.endsWith("-SNAPSHOT"))
    finatraModules ++ finatraExamples
  else
    finatraModules
}

lazy val root = (project in file(".")).
  settings(baseSettings).
  settings(buildSettings).
  settings(publishSettings).
  settings(unidocSettings).
  settings(
    organization := "com.twitter.finatra",
    moduleName := "finatra-root",
    unidocProjectFilter in(ScalaUnidoc, unidoc) := inAnyProject
      -- inProjects(benchmarks)
      // exclude example projects
      -- inProjects(benchmarkServer, exampleInjectJavaServer,
         helloWorld, helloWorldHeroku, streamingExample,
         thriftExampleIdl, thriftExampleServer,
         tinyUrl, twitterClone),
    aggregated := {
      println(aggregatedProjects.map(_.id).mkString("\n"))
    }
  ).aggregate(aggregatedProjects.map(x => x: ProjectReference): _*)

lazy val injectCore = (project in file("inject/inject-core")).
  settings(injectBuildSettings).
  settings(
    name := "inject-core",
    moduleName := "inject-core",
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
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.joda" % "joda-convert" % versions.jodaConvert,
      "com.google.inject" % "guice" % versions.guice % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test"
    )
  )

lazy val injectModules = (project in file("inject/inject-modules")).
  settings(injectBuildSettings).
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
  settings(injectBuildSettings).
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
  settings(injectBuildSettings).
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
    injectModules % "test->test",
    injectUtils,
    slf4j
  )

lazy val injectRequestScope = (project in file("inject/inject-request-scope")).
  settings(injectBuildSettings).
  settings(
    name := "inject-request-scope",
    moduleName := "inject-request-scope",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagle
    )
  ).
  dependsOn(
    injectCore,
    injectApp % "test->test",
    injectCore % "test->test"
  )

lazy val injectThriftClient = (project in file("inject/inject-thrift-client")).
  settings(injectBuildSettings).
  settings(
    name := "inject-thrift-client",
    moduleName := "inject-thrift-client",
    coverageExcludedPackages := "<empty>;com\\.twitter\\.test\\.thriftscala.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thrift" % versions.finagle,
      "com.twitter" %% "finagle-thriftmux" % versions.finagle,
      "com.twitter" %% "scrooge-core" % versions.scrooge,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.finagle % "test->compile")
  ).
  dependsOn(
    injectCore,
    injectUtils,
    injectCore % "test->test",
    injectApp % "test->test",
    http % "test->test",
    thrift % "test->test"
  )

lazy val injectUtils = (project in file("inject/inject-utils")).
  settings(injectBuildSettings).
  settings(
    name := "inject-utils",
    moduleName := "inject-utils",
    libraryDependencies ++= Seq(
      "commons-lang" % "commons-lang" % versions.commonsLang
    )
  ).
  dependsOn(
    injectCore,
    injectCore % "test->test"
  )

// Can run in the SBT console in this project with `> run -wi 20 -i 10 -f 1 .*`.
lazy val benchmarks = project.
  settings(baseServerBuildSettings).
  settings(jmhSettings).
  settings(
    name := "finatra-benchmarks",
    moduleName := "finatra-benchmarks",
    organization := "com.twitter.finatra",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j
    )
  ).
  dependsOn(
    http,
    injectRequestScope,
    injectCore % "test->test"
  )

lazy val utils = project.
  settings(finatraBuildSettings).
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
      "org.apache.thrift" % "libthrift" % versions.libThrift,
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.joda" % "joda-convert" % versions.jodaConvert
    )
  ).
  dependsOn(
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectUtils
  )

lazy val jackson = project.
  settings(finatraBuildSettings).
  settings(
    name := "finatra-jackson",
    moduleName := "finatra-jackson",
    coverageExcludedPackages := ".*CaseClassSigParser.*;.*JacksonToGuiceTypeConvertor.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
      "org.scala-lang" % "scalap" % scalaVersion.value exclude("org.scala-lang", "scala-compiler"),
      "com.twitter.finatra" %% "finatra-scalap-compiler-deps" % "2.0.0"
    )
  ).
  dependsOn(
    injectApp % "test->test",
    utils
  )

lazy val http = project.
  settings(finatraBuildSettings).
  settings(
    name := "finatra-http",
    moduleName := "finatra-http",
    coverageExcludedPackages := "<empty>;.*ScalaObjectHandler.*;com\\.twitter\\.finatra\\..*package.*;.*HttpReplyHandler.*",
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
    injectRequestScope,
    injectServer,
    httpclient % "test->test",
    jackson % "test->test",
    injectServer % "test->test"
  )

lazy val httpclient = project.
  settings(finatraBuildSettings).
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
  settings(finatraBuildSettings).
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
    injectCore,
    injectCore % "test->test"
  )

lazy val thrift = project.
  settings(finatraBuildSettings).
  settings(
    name := "finatra-thrift",
    moduleName := "finatra-thrift",
    coverageExcludedPackages := "<empty>;.*\\.thriftscala.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thriftmux" % versions.finagle,
      "org.yaml" % "snakeyaml" % versions.snakeyaml
    ),
    scroogePublishThrift in Compile := true,
    scroogeThriftIncludeFolders in Test := Seq(file("thrift/src/main/thrift")),
    excludeFilter in unmanagedResources := "BUILD"
  ).
  dependsOn(
    injectServer,
    utils,
    injectServer % "test->test"
  )

lazy val injectThriftClientHttpMapper = (project in file("inject-thrift-client-http-mapper")).
  settings(finatraBuildSettings).
  settings(
    name := "inject-thrift-client-http-mapper",
    moduleName := "inject-thrift-client-http-mapper",
    scroogeThriftIncludeFolders in Test := Seq(file("thrift/src/main/thrift")),
    excludeFilter in Test in unmanagedResources := "BUILD"
  ).
  dependsOn(
    http,
    injectCore,
    injectThriftClient,
    slf4j % "test->test",
    http % "test->test",
    injectServer % "test->test",
    injectThriftClient % "test->test",
    thrift % "test->compile",
    thrift % "test->test"
  )

// START EXAMPLES

// 2.11 only due to rlazoti/finagle-metrics dependency
lazy val helloWorldHeroku = (project in file("examples/hello-world-heroku")).
  settings(exampleServerBuildSettings).
  settings(
    name := "hello-world-heroku",
    moduleName := "hello-world-heroku",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq(),
    libraryDependencies ++= Seq(
      "com.github.rlazoti" % "finagle-metrics_2.11" % "0.0.2" //2.11 only
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    slf4j,
    injectCore % "test->test"
  )

lazy val helloWorld = (project in file("examples/hello-world")).
  settings(exampleServerBuildSettings).
  settings(
    name := "hello-world",
    moduleName := "hello-world",
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
  settings(exampleServerBuildSettings).
  settings(
    name := "streaming-example",
    moduleName := "streaming-example",
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

lazy val twitterClone = (project in file("examples/twitter-clone")).
  settings(exampleServerBuildSettings).
  settings(
    name := "twitter-clone",
    moduleName := "twitter-clone",
    coverageExcludedPackages := "<empty>;finatra\\.quickstart\\..*",
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

lazy val benchmarkServer = (project in file("examples/benchmark-server")).
  settings(baseServerBuildSettings).
  settings(
    name := "benchmark-server",
    moduleName := "benchmark-server",
    organization := "com.twitter.finatra.example"
  ).
  dependsOn(
    http,
    http % "test->test",
    httpclient,
    slf4j,
    injectCore % "test->test"
  )

lazy val tinyUrl = (project in file("examples/tiny-url")).
  settings(exampleServerBuildSettings).
  settings(
    name := "tiny-url",
    moduleName := "tiny-url",
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "2.7.2"
    )
  ).
  dependsOn(
    http,
    http % "test->test",
    httpclient,
    slf4j,
    injectCore % "test->test"
  )

lazy val exampleInjectJavaServer = (project in file("examples/java-server")).
  settings(exampleServerBuildSettings).
  settings(
    name := "java-server",
    moduleName := "java-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).
  dependsOn(
    slf4j,
    injectServer,
    injectServer % "test->test",
    injectCore % "test->test",
    injectApp % "test->test"
  )

lazy val thriftExampleIdl = (project in file("examples/thrift-server/thrift-example-idl")).
  settings(baseServerBuildSettings).
  settings(
    name := "thrift-example-idl",
    moduleName := "thrift-example-idl",
    coverageExcludedPackages := "<empty>;.*\\.thriftscala.*",
    scroogeThriftIncludeFolders in Compile := Seq(file("thrift/src/main/thrift"))
  ).
  dependsOn(thrift)

lazy val thriftExampleServer = (project in file("examples/thrift-server/thrift-example-server")).
  settings(exampleServerBuildSettings).
  settings(
    name := "thrift-example-server",
    moduleName := "thrift-example-server",
    coverageExcludedPackages := "<empty>;.*ExceptionTranslationFilter.*",
    scroogeThriftIncludeFolders in Compile := Seq(
      file("thrift/src/main/thrift"),
      file("examples/thrift-server/thrift-example-idl/src/main/thrift"))
  ).
  dependsOn(
    thriftExampleIdl,
    slf4j,
    thrift,
    thrift % "test->test",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test"
  )

// END EXAMPLES
