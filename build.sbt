import sbt.Keys._
import sbtunidoc.Plugin.UnidocKeys._
import scala.language.reflectiveCalls
import scoverage.ScoverageKeys

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

lazy val projectVersion = "2.8.0-SNAPSHOT"

lazy val buildSettings = Seq(
  version := projectVersion,
  scalaVersion := "2.11.8",
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
  fork in Test := true,
  javaOptions in Test ++= travisTestJavaOptions
)

def travisTestJavaOptions: Seq[String] = {
  // When building on travis-ci, we want to suppress logging to error level only.
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=error", 
      "-Dcom.twitter.inject.test.logging.disabled",
      // Needed to avoid cryptic EOFException crashes in forked tests
      // in Travis with `sudo: false`.
      // See https://github.com/sbt/sbt/issues/653
      // and https://github.com/travis-ci/travis-ci/issues/3775
      "-Xmx3G")
  } else Seq.empty
}

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")
  val branch = Process("git" :: "rev-parse" :: "--abbrev-ref" :: "HEAD" :: Nil).!!.trim
  val suffix = if (branch == "master" || travisBranch == "master") "" else "-SNAPSHOT"

  // Use SNAPSHOT versions of Twitter libraries on non-master branches
  val finagleVersion = "6.41.0" + suffix
  val scroogeVersion = "4.13.0" + suffix
  val twitterserverVersion = "1.26.0" + suffix
  val utilVersion = "6.40.0" + suffix

  val bijectionVersion = "0.9.4"
  val commonsCodec = "1.9"
  val commonsFileupload = "1.3.1"
  val commonsIo = "2.4"
  val commonsLang = "2.6"
  val grizzled = "1.3.0"
  val guava = "16.0.1"
  val guice = "4.0"
  val jackson = "2.8.4"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val libThrift = "0.5.0-7"
  val logback = "1.1.7"
  val mockito = "1.9.5"
  val mustache = "0.8.18"
  val nscalaTime = "1.6.0"
  val scalaCheck = "1.13.4"
  val scalaGuice = "4.1.0"
  val scalaTest = "3.0.0"
  val servletApi = "2.5"
  val slf4j = "1.7.21"
  val snakeyaml = "1.12"
  val specs2 = "2.3.12"
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
  "-Xlint",
  "-Ywarn-unused-import"
)

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" %  versions.mockito % "test",
    "org.scalacheck" %% "scalacheck" % versions.scalaCheck % "test",
    "org.scalatest" %% "scalatest" %  versions.scalaTest % "test",
    "org.specs2" %% "specs2" % versions.specs2 % "test"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalaCompilerOptions,
  javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in doc ++= Seq("-source", "1.8")
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in (Compile, packageDoc) := true,
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
  apiURL := Some(url("https://twitter.github.io/finatra/scaladocs/")),
  excludeFilter in (Compile, managedSources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, unmanagedSources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, managedResources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, unmanagedResources) := HiddenFileFilter || "BUILD",
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
  },
  resourceGenerators in Compile <+=
    (resourceManaged in Compile, name, version) map { (dir, name, ver) =>
      val file = dir / "com" / "twitter" / name / "build.properties"
      val buildRev = Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
      val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
      val contents = s"name=$name\nversion=$ver\nbuild_revision=$buildRev\nbuild_name=$buildName"
      IO.write(file, contents)
      Seq(file)
    }
)

lazy val slf4jSimpleTestDependency = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % versions.slf4j % "test"
  )
)

lazy val projectSettings = baseSettings ++ buildSettings ++ publishSettings ++ slf4jSimpleTestDependency ++ Seq(
  organization := "com.twitter"
)

lazy val baseServerSettings = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  organization := "com.twitter",
  publishArtifact := false,
  publishLocal := {},
  publish := {},
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val exampleServerSettings = baseServerSettings ++ Seq(
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback)
)

lazy val finatraModules = Seq[sbt.ProjectReference](
  benchmarks,
  http,
  httpclient,
  injectApp,
  injectCore,
  injectModules,
  injectRequestScope,
  injectServer,
  injectThrift,
  injectThriftClient,
  injectThriftClientHttpMapper,
  injectSlf4j,
  injectUtils,
  jackson,
  slf4j,
  thrift,
  utils)

lazy val finatraExamples =
  // START EXAMPLES
  Seq[sbt.ProjectReference](
    benchmarkServer,
    exampleHttpJavaServer,
    exampleInjectJavaServer,
    exampleWebDashboard,
    helloWorld,
    helloWorldHeroku,
    streamingExample,
    thriftExampleServer,
    thriftJavaExampleServer,
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

def mappingContainsAnyPath(mapping: (File, String), paths: Seq[String]): Boolean = {
   paths.foldLeft(false)(_ || mapping._1.getPath.contains(_))
}

lazy val root = (project in file("."))
  .settings(baseSettings)
  .settings(buildSettings)
  .settings(publishSettings)
  .settings(unidocSettings)
  .settings(
    organization := "com.twitter",
    moduleName := "finatra-root",
    unidocProjectFilter in(ScalaUnidoc, unidoc) := inAnyProject
      -- inProjects(benchmarks)
      // START EXAMPLES
      -- inProjects(benchmarkServer, exampleHttpJavaServer, exampleInjectJavaServer, exampleWebDashboard,
         helloWorld, helloWorldHeroku, streamingExample,
         thriftExampleIdl, thriftExampleServer, thriftJavaExampleIdl, thriftJavaExampleServer,
         tinyUrl, twitterClone)
      // END EXAMPLES
  ).aggregate(aggregatedProjects: _*)

lazy val injectCore = (project in file("inject/inject-core"))
  .settings(projectSettings)
  .settings(
    name := "inject-core",
    moduleName := "inject-core",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % versions.jackson,
      "com.google.guava" % "guava" % versions.guava,
      "com.google.inject" % "guice" % versions.guice,
      "com.google.inject.extensions" % "guice-assistedinject" % versions.guice,
      "com.google.inject.extensions" % "guice-multibindings" % versions.guice,
      "com.twitter" %% "util-app" % versions.utilVersion,
      "com.twitter" %% "util-core" % versions.utilVersion,
      "commons-io" % "commons-io" % versions.commonsIo,
      "javax.inject" % "javax.inject" % "1",
      "joda-time" % "joda-time" % versions.jodaTime,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.joda" % "joda-convert" % versions.jodaConvert,
      "com.google.inject" % "guice" % versions.guice % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test"
    ),
    publishArtifact in (Test, packageBin) := true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/inject/IntegrationTest",
              "com/twitter/inject/Mockito",
              "com/twitter/inject/PoolUtils",
              "com/twitter/inject/Resettable",
              "com/twitter/inject/Test",
              "com/twitter/inject/TwitterTestModule",
              "org/specs2/matcher/ScalaTestExpectations")))
    }
  )

lazy val injectModules = (project in file("inject/inject-modules"))
  .settings(projectSettings)
  .settings(
    name := "inject-modules",
    moduleName := "inject-modules",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagleVersion,
      "com.twitter" %% "util-stats" % versions.utilVersion
    ),
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/inject/modules/InMemoryStatsReceiverModule")))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile")

lazy val injectApp = (project in file("inject/inject-app"))
  .settings(projectSettings)
  .settings(
    name := "inject-app",
    moduleName := "inject-app",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions.utilVersion
    ),
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*TypeConverter.*",
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/inject/app/Banner",
              "com/twitter/inject/app/EmbeddedApp",
              "com/twitter/inject/app/FeatureTest",
              "com/twitter/inject/app/InjectionServiceModule",
              "com/twitter/inject/app/StartupTimeoutException",
              "com/twitter/inject/app/TestInjector")))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile")

lazy val injectServer = (project in file("inject/inject-server"))
  .settings(projectSettings)
  .settings(
    name := "inject-server",
    moduleName := "inject-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Ports.*;.*FinagleBuildRevision.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-stats" % versions.finagleVersion,
      "com.twitter" %% "twitter-server" % versions.twitterserverVersion
    ),
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/inject/server/EmbeddedTwitterServer",
              "com/twitter/inject/server/FeatureTest")))
    }
  ).dependsOn(
    injectApp % "test->test;compile->compile",
    injectModules % "test->test;compile->compile",
    injectSlf4j,
    injectUtils)

lazy val injectSlf4j = (project in file("inject/inject-slf4j"))
  .settings(projectSettings)
  .settings(
    name := "inject-slf4j",
    moduleName := "inject-slf4j",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*LoggerModule.*;.*Slf4jBridgeUtility.*",
    libraryDependencies ++= Seq(
      "org.clapper" %% "grizzled-slf4j" % versions.grizzled,
      "org.slf4j" % "jcl-over-slf4j" % versions.slf4j,
      "org.slf4j" % "jul-to-slf4j" % versions.slf4j,
      "org.slf4j" % "log4j-over-slf4j" % versions.slf4j,
      "org.slf4j" % "slf4j-api" % versions.slf4j
    )
  ).dependsOn(
    injectCore)

lazy val injectRequestScope = (project in file("inject/inject-request-scope"))
  .settings(projectSettings)
  .settings(
    name := "inject-request-scope",
    moduleName := "inject-request-scope",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagleVersion
    )
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectApp % "test->test")

lazy val injectThrift = (project in file("inject/inject-thrift"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift",
    moduleName := "inject-thrift",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    libraryDependencies ++= Seq(
      "com.twitter" % "libthrift" % versions.libThrift,
      "com.twitter" %% "finagle-core" % versions.finagleVersion,
      "com.twitter" %% "finagle-mux" % versions.finagleVersion,
      "com.twitter" %% "scrooge-core" % versions.scroogeVersion,
      "com.twitter" %% "util-core" % versions.utilVersion),
    resolvers += "twitter-repo" at "https://maven.twttr.com"
  ).dependsOn(
    injectCore % "test->test",
    injectUtils)

lazy val injectThriftClient = (project in file("inject/inject-thrift-client"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift-client",
    moduleName := "inject-thrift-client",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-exp" % versions.finagleVersion,
      "com.twitter" %% "finagle-thrift" % versions.finagleVersion,
      "com.twitter" %% "finagle-thriftmux" % versions.finagleVersion,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.finagleVersion % "test->compile")
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectUtils,
    injectApp % "test->test;compile->compile",
    injectThrift,
    http % "test->test",
    thrift % "test->test")

lazy val injectUtils = (project in file("inject/inject-utils"))
  .settings(projectSettings)
  .settings(
    name := "inject-utils",
    moduleName := "inject-utils",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagleVersion,
      "com.twitter" %% "finagle-mux" % versions.finagleVersion,
      "com.twitter" %% "util-core" % versions.utilVersion,
      "commons-lang" % "commons-lang" % versions.commonsLang
    )
  ).dependsOn(
    injectCore % "test->test;compile->compile")

/**
 * This project relies on other projects test dependencies and as such
 * needs to have all of the benchmarks defined in test scope to play
 * well with IDEs other build systems.
 *
 * @see https://github.com/ktoso/sbt-jmh/issues/63
 *
 * To run, open sbt console:
 * $ ./sbt
 * > project benchmarks
 * > run -wi 20 -i 10 -f 1 .*`.
 */
lazy val benchmarks = project
  .settings(baseServerSettings)
  .enablePlugins(JmhPlugin)
  .dependsOn(
    http,
    injectRequestScope,
    injectCore % "test->test",
    injectApp % "test->test;compile->compile")

lazy val utils = project
  .settings(projectSettings)
  .settings(
    name := "finatra-utils",
    moduleName := "finatra-utils",
    ScoverageKeys.coverageExcludedPackages := "<empty>;com\\.twitter\\.finatra\\..*package.*;.*ClassUtils.*;.*WrappedValue.*;.*DeadlineValues.*;.*RichBuf.*;.*RichByteBuf.*",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % versions.guice,
      "joda-time" % "joda-time" % versions.jodaTime,
      "commons-io" % "commons-io" % versions.commonsIo,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" % "libthrift" % versions.libThrift,
      "com.twitter" %% "finagle-http" % versions.finagleVersion,
      "com.twitter" %% "util-core" % versions.utilVersion
    ),
    resolvers += "twitter-repo" at "https://maven.twttr.com",
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/finatra/modules/",
              "com/twitter/finatra/test/")))
    }
  ).dependsOn(
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectUtils)

lazy val jackson = project
  .settings(projectSettings)
  .settings(
    name := "finatra-jackson",
    moduleName := "finatra-jackson",
    ScoverageKeys.coverageExcludedPackages := ".*CaseClassSigParser.*;.*JacksonToGuiceTypeConverter.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
      "org.scala-lang" % "scalap" % scalaVersion.value exclude("org.scala-lang", "scala-compiler"),
      "com.twitter.finatra" %% "finatra-scalap-compiler-deps" % "2.0.0"
    ),
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/finatra/json/JsonDiff",
              "com/twitter/finatra/validation/")))
    }
  ).dependsOn(
    injectApp % "test->test",
    utils)

lazy val http = project
  .settings(projectSettings)
  .settings(
    name := "finatra-http",
    moduleName := "finatra-http",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ScalaObjectHandler.*;.*NonValidatingHttpHeadersResponse.*;com\\.twitter\\.finatra\\..*package.*",
    libraryDependencies ++= Seq(
      "com.github.spullara.mustache.java" % "compiler" % versions.mustache,
      "com.twitter" %% "bijection-util" % versions.bijectionVersion,
      "com.twitter" %% "finagle-exp" % versions.finagleVersion,
      "com.twitter" %% "finagle-http" % versions.finagleVersion,
      "commons-fileupload" % "commons-fileupload" % versions.commonsFileupload,
      "javax.servlet" % "servlet-api" % versions.servletApi
    ),
    unmanagedResourceDirectories in Test <+= baseDirectory(
      _ / "src" / "test" / "webapp"
    ),
    excludeFilter in Test in unmanagedResources := "BUILD",
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/finatra/http/EmbeddedHttpServer",
              "com/twitter/finatra/http/HttpMockResponses",
              "com/twitter/finatra/http/HttpTest",
              "com/twitter/finatra/http/StreamingJsonTestHelper")))
    }
  ).dependsOn(
    jackson % "test->test;compile->compile",
    injectRequestScope,
    injectServer % "test->test;compile->compile",
    httpclient % "test->test",
    slf4j)

lazy val httpclient = project
  .settings(projectSettings)
  .settings(
    name := "finatra-httpclient",
    moduleName := "finatra-httpclient",
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % versions.commonsCodec
    ),
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/finatra/httpclient/test/")))
    }
  ).dependsOn(
    jackson,
    utils % "test->test",
    injectApp % "test->test")

lazy val slf4j = project
  .settings(projectSettings)
  .settings(
    name := "finatra-slf4j",
    moduleName := "finatra-slf4j",
    ScoverageKeys.coverageExcludedPackages := "<empty>;org\\.slf4j\\..*package.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions.utilVersion
    )
  ).dependsOn(
    injectSlf4j,
    injectCore % "test->test;compile->compile")

lazy val thrift = project
  .settings(projectSettings)
  .settings(
    name := "finatra-thrift",
    moduleName := "finatra-thrift",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.finagleVersion,
      "com.twitter" %% "finagle-exp" % versions.finagleVersion,
      "com.twitter" %% "finagle-thrift" % versions.finagleVersion,
      "com.twitter" %% "finagle-thriftmux" % versions.finagleVersion,
      "com.twitter" %% "util-core" % versions.utilVersion,
      "javax.inject" % "javax.inject" % "1",
      "org.yaml" % "snakeyaml" % versions.snakeyaml
    ),
    scroogePublishThrift in Compile := true,
    scroogeThriftIncludeFolders in Test := Seq(file("thrift/src/main/thrift")),
    scroogeLanguages in Compile := Seq("java", "scala"),
    scroogeLanguages in Test := Seq("java", "scala"),
    excludeFilter in unmanagedResources := "BUILD",
    publishArtifact in (Test, packageBin):= true,
    publishArtifact in (Test, packageDoc) := true,
    mappings in (Test, packageBin) ~= { fileMappings: Seq[(File, String)] =>
      fileMappings.filter(
        mappingContainsAnyPath(_,
          Seq("com/twitter/finatra/thrift/EmbeddedThriftServer",
              "com/twitter/finatra/thrift/ThriftClient",
              "com/twitter/finatra/thrift/ThriftTest")))
    }
  ).dependsOn(
    injectServer % "test->test;compile->compile",
    injectThrift,
    utils,
    slf4j)

lazy val injectThriftClientHttpMapper = (project in file("inject-thrift-client-http-mapper"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift-client-http-mapper",
    moduleName := "inject-thrift-client-http-mapper",
    scroogeThriftIncludeFolders in Test := Seq(file("thrift/src/main/thrift")),
    excludeFilter in Test in unmanagedResources := "BUILD"
  ).dependsOn(
    http % "test->test;compile->compile",
    injectCore,
    injectThriftClient % "test->test;compile->compile",
    slf4j % "test->test",
    injectServer % "test->test",
    thrift % "test->test;test->compile")

// START EXAMPLES

lazy val helloWorldHeroku = (project in file("examples/hello-world-heroku"))
  .settings(exampleServerSettings)
  .settings(
    name := "hello-world-heroku",
    moduleName := "hello-world-heroku",
    libraryDependencies ++= Seq(
      "com.github.rlazoti" %% "finagle-metrics" % "0.0.3"
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    slf4j,
    injectCore % "test->test")

lazy val helloWorld = (project in file("examples/hello-world"))
  .settings(exampleServerSettings)
  .settings(
    name := "hello-world",
    moduleName := "hello-world"
  ).dependsOn(
    http % "test->test;compile->compile",
    slf4j,
    injectCore % "test->test")

lazy val streamingExample = (project in file("examples/streaming-example"))
  .settings(exampleServerSettings)
  .settings(
    name := "streaming-example",
    moduleName := "streaming-example",
    libraryDependencies ++= Seq(
      "com.twitter" % "joauth" % "6.0.2"
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    slf4j,
    injectCore % "test->test")

lazy val twitterClone = (project in file("examples/twitter-clone"))
  .settings(exampleServerSettings)
  .settings(
    name := "twitter-clone",
    moduleName := "twitter-clone",
    ScoverageKeys.coverageExcludedPackages := "<empty>;finatra\\.quickstart\\..*"
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    slf4j,
    injectCore % "test->test")

lazy val benchmarkServer = (project in file("examples/benchmark-server"))
  .settings(baseServerSettings)
  .settings(
    name := "benchmark-server",
    moduleName := "benchmark-server",
    mainClass in Compile := Some("com.twitter.finatra.http.benchmark.FinatraBenchmarkServerMain"),
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-nop" % versions.slf4j
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    injectCore % "test->test")

lazy val tinyUrl = (project in file("examples/tiny-url"))
  .settings(exampleServerSettings)
  .settings(
    name := "tiny-url",
    moduleName := "tiny-url",
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "2.7.2"
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    slf4j,
    injectCore % "test->test")

lazy val exampleHttpJavaServer = (project in file("examples/java-http-server"))
  .settings(exampleServerSettings)
  .settings(
    name := "java-http-server",
    moduleName := "java-http-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    slf4j,
    injectCore % "test->test")

lazy val exampleInjectJavaServer = (project in file("examples/java-server"))
  .settings(exampleServerSettings)
  .settings(
    name := "java-server",
    moduleName := "java-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).dependsOn(
    slf4j,
    injectServer % "test->test;compile->compile",
    injectCore % "test->test",
    injectApp % "test->test")

lazy val thriftExampleIdl = (project in file("examples/thrift-server/thrift-example-idl"))
  .settings(baseServerSettings)
  .settings(
    name := "thrift-example-idl",
    moduleName := "thrift-example-idl",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*",
    scroogeThriftIncludeFolders in Compile := Seq(
      file("thrift/src/main/thrift"),
      file("examples/thrift-server/thrift-example-idl/src/main/thrift"))
  ).dependsOn(thrift)

lazy val thriftExampleServer = (project in file("examples/thrift-server/thrift-example-server"))
  .settings(exampleServerSettings)
  .settings(
    name := "thrift-example-server",
    moduleName := "thrift-example-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ExceptionTranslationFilter.*"
  ).dependsOn(
    thriftExampleIdl,
    slf4j,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test")

lazy val thriftJavaExampleIdl = (project in file("examples/java-thrift-server/thrift-example-idl"))
  .settings(baseServerSettings)
  .settings(
    name := "java-thrift-example-idl",
    moduleName := "java-thrift-example-idl",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftjava.*",
    scroogeLanguages in Compile := Seq("java"),
    scroogeThriftIncludeFolders in Compile := Seq(
      file("thrift/src/main/thrift"),
      file("examples/java-thrift-server/thrift-example-idl/src/main/thrift"))
  ).dependsOn(thrift)

lazy val thriftJavaExampleServer = (project in file("examples/java-thrift-server/thrift-example-server"))
  .settings(exampleServerSettings)
  .settings(
    name := "java-thrift-example-server",
    moduleName := "java-thrift-example-server"
  ).dependsOn(
    thriftJavaExampleIdl,
    slf4j,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test")

lazy val exampleWebDashboard = (project in file("examples/web-dashboard")).
  settings(exampleServerSettings).
  settings(
    name := "web-dashboard",
    moduleName := "web-dashboard",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src" / "main" / "webapp"
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    slf4j,
    injectCore % "test->test")

// END EXAMPLES
