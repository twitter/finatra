import scala.language.reflectiveCalls
import scoverage.ScoverageKeys

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

// All Twitter library releases are date versioned as YY.MM.patch
val releaseVersion = "18.5.0-SNAPSHOT"

lazy val buildSettings = Seq(
  version := releaseVersion,
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.11", "2.12.4"),
  scalaModuleInfo := scalaModuleInfo.value.map(_.withOverrideScalaVersion(true)),
  fork in Test := true,
  javaOptions in Test ++= travisTestJavaOptions
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  // sbt-pgp's publishSigned task needs this defined even though it is not publishing.
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

def travisTestJavaOptions: Seq[String] = {
  // When building on travis-ci, we want to suppress logging to error level only.
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-DSKIP_FLAKY=true",
      "-Dsbt.log.noformat=true",
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=error",
      "-Dcom.twitter.inject.test.logging.disabled",
      // Needed to avoid cryptic EOFException crashes in forked tests
      // in Travis with `sudo: false`.
      // See https://github.com/sbt/sbt/issues/653
      // and https://github.com/travis-ci/travis-ci/issues/3775
      "-Xmx3G")
  } else {
    Seq(
      "-DSKIP_FLAKY=true")
  }
}

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")

  // All Twitter library releases are date versioned as YY.MM.patch
  val twLibVersion = releaseVersion

  val commonsCodec = "1.9"
  val commonsFileupload = "1.3.1"
  val commonsIo = "2.4"
  val commonsLang = "2.6"
  val guava = "19.0"
  val guice = "4.0"
  val jackson = "2.8.4"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val junit = "4.12"
  val libThrift = "0.10.0"
  val logback = "1.1.7"
  val mockito = "1.9.5"
  val mustache = "0.8.18"
  val nscalaTime = "2.14.0"
  val scalaCheck = "1.13.4"
  val scalaGuice = "4.1.0"
  val scalaTest = "3.0.0"
  val servletApi = "2.5"
  val slf4j = "1.7.21"
  val snakeyaml = "1.12"
  val specs2 = "2.4.17"
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
    "org.mockito" % "mockito-core" %  versions.mockito % Test,
    "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test,
    "org.scalatest" %% "scalatest" %  versions.scalaTest % Test,
    "org.specs2" %% "specs2-core" % versions.specs2 % Test,
    "org.specs2" %% "specs2-junit" % versions.specs2 % Test,
    "org.specs2" %% "specs2-mock" % versions.specs2 % Test
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalaCompilerOptions,
  javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in doc ++= Seq("-source", "1.8"),
  // -a: print stack traces for failing asserts
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
  // broken in 2.12 due to: https://issues.scala-lang.org/browse/SI-10134
  scalacOptions in (Compile, doc) ++= {
    if (scalaVersion.value.startsWith("2.12")) Seq("-no-java-comments")
    else Nil
  }
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Compile := true,
  publishArtifact in Test := false,
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
  resourceGenerators in Compile += Def.task {
    val dir = (resourceManaged in Compile).value
    val file = dir / "com" / "twitter" / name.value / "build.properties"
    val buildRev = scala.sys.process.Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
    val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
    val contents = s"name=${name.value}\nversion=${version.value}\nbuild_revision=$buildRev\nbuild_name=$buildName"
    IO.write(file, contents)
    Seq(file)
  }.taskValue
)

lazy val slf4jSimpleTestDependency = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % versions.slf4j % Test
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
  fork in run := true,
  javaOptions in Test ++= Seq("-Dlog.service.output=/dev/stdout", "-Dlog.access.output=/dev/stdout", "-Dlog_level=INFO"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "twitter-server-logback-classic" % versions.twLibVersion,
    "ch.qos.logback" % "logback-classic" % versions.logback
  )
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
  thrift,
  utils)

lazy val finatraExamples =
  // START EXAMPLES
  Seq[sbt.ProjectReference](
    benchmarkServer,
    exampleHttpJavaServer,
    exampleInjectJavaServer,
    exampleTwitterServer,
    exampleWebDashboard,
    helloWorld,
    streamingExample,
    thriftExampleServer,
    thriftJavaExampleServer,
    twitterClone) ++
  // END EXAMPLES
  Seq.empty

def aggregatedProjects = finatraModules ++ finatraExamples

def mappingContainsAnyPath(mapping: (File, String), paths: Seq[String]): Boolean = {
   paths.foldLeft(false)(_ || mapping._1.getPath.contains(_))
}

lazy val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(baseSettings)
  .settings(buildSettings)
  .settings(noPublishSettings)
  .settings(
    organization := "com.twitter",
    moduleName := "finatra-root",
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject
      -- inProjects(benchmarks)
      // START EXAMPLES
      -- inProjects(benchmarkServer, exampleHttpJavaServer, exampleInjectJavaServer,
         exampleTwitterServer, exampleWebDashboard, helloWorld,
         streamingExample, thriftExampleIdl, thriftExampleServer,
         thriftJavaExampleIdl, thriftJavaExampleServer, twitterClone)
      // END EXAMPLES
  ).aggregate(aggregatedProjects: _*)

lazy val injectCoreTestJarSources =
  Seq("com/twitter/inject/IntegrationTest",
    "com/twitter/inject/IntegrationTestMixin",
    "com/twitter/inject/Mockito",
    "com/twitter/inject/PoolUtils",
    "com/twitter/inject/Resettable",
    "com/twitter/inject/Test",
    "com/twitter/inject/TestMixin",
    "com/twitter/inject/TwitterTestModule",
    "com/twitter/inject/WordSpecIntegrationTest",
    "com/twitter/inject/WordSpecTest",
    "org/specs2/matcher/ScalaTestExpectations")
lazy val injectCore = (project in file("inject/inject-core"))
  .settings(projectSettings)
  .settings(
    name := "inject-core",
    moduleName := "inject-core",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % versions.guice,
      "com.google.inject.extensions" % "guice-assistedinject" % versions.guice,
      "com.google.inject.extensions" % "guice-multibindings" % versions.guice,
      "com.twitter" %% "util-app" % versions.twLibVersion,
      "commons-io" % "commons-io" % versions.commonsIo,
      "javax.inject" % "javax.inject" % "1",
      "joda-time" % "joda-time" % versions.jodaTime,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.joda" % "joda-convert" % versions.jodaConvert,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "com.google.inject" % "guice" % versions.guice % Test,
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % Test
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, injectCoreTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, injectCoreTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, injectCoreTestJarSources))
    }
  ).dependsOn(
    injectSlf4j)

lazy val injectModulesTestJarSources =
  Seq("com/twitter/inject/modules/InMemoryStatsReceiverModule")
lazy val injectModules = (project in file("inject/inject-modules"))
  .settings(projectSettings)
  .settings(
    name := "inject-modules",
    moduleName := "inject-modules",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-slf4j-jul-bridge" % versions.twLibVersion,
      "com.twitter" %% "util-stats" % versions.twLibVersion
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, injectModulesTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, injectModulesTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, injectModulesTestJarSources))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile")

lazy val injectAppTestJarSources =
  Seq("com/twitter/inject/app/Banner",
    "com/twitter/inject/app/BindDSL",
    "com/twitter/inject/app/EmbeddedApp",
    "com/twitter/inject/app/InjectionServiceModule",
    "com/twitter/inject/app/InjectionServiceWithAnnotationModule",
    "com/twitter/inject/app/StartupTimeoutException",
    "com/twitter/inject/app/TestInjector")
lazy val injectApp = (project in file("inject/inject-app"))
  .settings(projectSettings)
  .settings(
    name := "inject-app",
    moduleName := "inject-app",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*TypeConverter.*",
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, injectAppTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, injectAppTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, injectAppTestJarSources))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectModules % Test)

lazy val injectServerTestJarSources =
  Seq(
    "com/twitter/inject/server/AdminHttpClient",
    "com/twitter/inject/server/EmbeddedHttpClient",
    "com/twitter/inject/server/EmbeddedTwitterServer",
    "com/twitter/inject/server/FeatureTest",
    "com/twitter/inject/server/FeatureTestMixin",
    "com/twitter/inject/server/package",
    "com/twitter/inject/server/WordSpecFeatureTest")
lazy val injectServer = (project in file("inject/inject-server"))
  .settings(projectSettings)
  .settings(
    name := "inject-server",
    moduleName := "inject-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Ports.*;.*FinagleBuildRevision.*",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % versions.guava % Test,
      "com.twitter" %% "finagle-stats" % versions.twLibVersion,
      "com.twitter" %% "twitter-server" % versions.twLibVersion
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, injectServerTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, injectServerTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, injectServerTestJarSources))
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
    ScoverageKeys.coverageExcludedPackages := "<empty>;",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % versions.jackson,
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-slf4j-api" % versions.twLibVersion,
      "org.slf4j" % "jcl-over-slf4j" % versions.slf4j,
      "org.slf4j" % "jul-to-slf4j" % versions.slf4j,
      "org.slf4j" % "log4j-over-slf4j" % versions.slf4j,
      "org.slf4j" % "slf4j-api" % versions.slf4j)
  )

lazy val injectRequestScope = (project in file("inject/inject-request-scope"))
  .settings(projectSettings)
  .settings(
    name := "inject-request-scope",
    moduleName := "inject-request-scope",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion
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
      "org.apache.thrift" % "libthrift" % versions.libThrift,
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-mux" % versions.twLibVersion,
      "com.twitter" %% "scrooge-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion)
  ).dependsOn(
    injectCore % "test->test",
    injectUtils)

lazy val injectThriftClient = (project in file("inject/inject-thrift-client"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift-client",
    moduleName := "inject-thrift-client",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*;.*LatencyFilter.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-exp" % versions.twLibVersion,
      "com.twitter" %% "finagle-thrift" % versions.twLibVersion,
      "com.twitter" %% "finagle-thriftmux" % versions.twLibVersion,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.twLibVersion % Test)
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
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
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
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % Test))
  .settings(noPublishSettings)
  .dependsOn(
    http,
    injectRequestScope,
    injectCore % "test->test",
    injectApp % "test->test;compile->compile")

lazy val utilsTestJarSources =
  Seq("com/twitter/finatra/modules/",
    "com/twitter/finatra/test/")
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
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, utilsTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, utilsTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, utilsTestJarSources))
    }
  ).dependsOn(
    injectApp % "test->test;compile->compile",
    injectCore % "test->test",
    injectServer % "test->test",
    injectUtils)

lazy val jacksonTestJarSources =
  Seq(
    "com/twitter/finatra/validation",
    "com/twitter/finatra/json/JsonDiff")
lazy val jackson = project
  .settings(projectSettings)
  .settings(
    name := "finatra-jackson",
    moduleName := "finatra-jackson",
    ScoverageKeys.coverageExcludedPackages := ".*CaseClassSigParser.*;.*JacksonToGuiceTypeConverter.*;.*DurationMillisSerializer.*;.*ByteBufferUtils.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion
    ),
    // special-case to only scaladoc what's necessary as some of the tests cannot generate scaladocs
    sources in Test in doc := {
      val previous: Seq[File] = (sources in Test in doc).value
      previous.filter(file => jacksonTestJarSources.foldLeft(false)(_ || file.getPath.contains(_)))
    },
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, jacksonTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, jacksonTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, jacksonTestJarSources))
    }
  ).dependsOn(
    injectApp % "test->test",
    injectUtils)

lazy val httpTestJarSources =
  Seq(
    "com/twitter/finatra/http/EmbeddedHttpServer",
    "com/twitter/finatra/http/ExternalHttpClient",
    "com/twitter/finatra/http/HttpMockResponses",
    "com/twitter/finatra/http/HttpTest",
    "com/twitter/finatra/http/JsonAwareEmbeddedHttpClient",
    "com/twitter/finatra/http/StreamingJsonTestHelper")
lazy val http = project
  .settings(projectSettings)
  .settings(
    name := "finatra-http",
    moduleName := "finatra-http",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ScalaObjectHandler.*;.*NonValidatingHttpHeadersResponse.*;com\\.twitter\\.finatra\\..*package.*;.*ThriftExceptionMapper.*;.*HttpResponseExceptionMapper.*;.*HttpResponseException.*",
    libraryDependencies ++= Seq(
      "com.github.spullara.mustache.java" % "compiler" % versions.mustache exclude("com.google.guava", "guava"),
      "com.google.guava" % "guava" % versions.guava,
      "com.twitter" %% "finagle-exp" % versions.twLibVersion,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "commons-fileupload" % "commons-fileupload" % versions.commonsFileupload,
      "javax.servlet" % "servlet-api" % versions.servletApi,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    unmanagedResourceDirectories in Test += baseDirectory(
      _ / "src" / "test" / "webapp"
    ).value,
    excludeFilter in Test in unmanagedResources := "BUILD",
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, httpTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, httpTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, httpTestJarSources))
    }
  ).dependsOn(
    jackson % "test->test;compile->compile",
    injectRequestScope % Test,
    injectSlf4j,
    injectServer % "test->test;compile->compile",
    httpclient % "test->test",
    utils % "test->test;compile->compile")

lazy val httpclientTestJarSources =
  Seq("com/twitter/finatra/httpclient/test/")
lazy val httpclient = project
  .settings(projectSettings)
  .settings(
    name := "finatra-httpclient",
    moduleName := "finatra-httpclient",
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % versions.commonsCodec,
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "twitter-server" % versions.twLibVersion % Test,
      "com.twitter" %% "util-core" % versions.twLibVersion
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, httpclientTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, httpclientTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, httpclientTestJarSources))
    }
  ).dependsOn(
    jackson,
    injectUtils,
    injectApp % "test->test",
    injectCore % "test->test")

lazy val thriftTestJarSources =
  Seq("com/twitter/finatra/thrift/EmbeddedThriftServer",
    "com/twitter/finatra/thrift/ThriftClient",
    "com/twitter/finatra/thrift/ThriftTest")
lazy val thrift = project
  .settings(projectSettings)
  .settings(
    name := "finatra-thrift",
    moduleName := "finatra-thrift",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-exp" % versions.twLibVersion,
      "com.twitter" %% "finagle-thrift" % versions.twLibVersion,
      "com.twitter" %% "finagle-thriftmux" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "javax.inject" % "javax.inject" % "1",
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.yaml" % "snakeyaml" % versions.snakeyaml
    ),
    scroogePublishThrift in Compile := true,
    scroogeThriftIncludeFolders in Test := Seq(file("thrift/src/main/thrift")),
    scroogeLanguages in Compile := Seq("java", "scala"),
    scroogeLanguages in Test := Seq("java", "scala"),
    excludeFilter in unmanagedResources := "BUILD",
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, thriftTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, thriftTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, thriftTestJarSources))
    }
  ).dependsOn(
    injectServer % "test->test;compile->compile",
    injectSlf4j,
    injectThrift,
    utils)

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
    injectServer % "test->test",
    injectSlf4j % "test->test",
    injectThriftClient % "test->test;compile->compile",
    thrift % "test->test;test->compile")

lazy val site = (project in file("doc"))
  .enablePlugins(SphinxPlugin)
  .settings(
      baseSettings ++ buildSettings ++ Seq(
        scalacOptions in doc ++= Seq("-doc-title", "Finatra", "-doc-version", version.value),
        includeFilter in Sphinx := ("*.html" | "*.png" | "*.svg" | "*.js" | "*.css" | "*.gif" | "*.txt")))

// START EXAMPLES

lazy val helloWorld = (project in file("examples/hello-world"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "hello-world",
    moduleName := "hello-world"
  ).dependsOn(
    http % "test->test;compile->compile",
    injectCore % "test->test",
    injectSlf4j)

lazy val streamingExample = (project in file("examples/streaming-example"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "streaming-example",
    moduleName := "streaming-example",
    libraryDependencies ++= Seq(
      "com.twitter" % "joauth" % "6.0.2"
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    injectCore % "test->test",
    injectSlf4j)

lazy val twitterClone = (project in file("examples/twitter-clone"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "twitter-clone",
    moduleName := "twitter-clone",
    ScoverageKeys.coverageExcludedPackages := "<empty>;finatra\\.quickstart\\..*"
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    injectCore % "test->test",
    injectSlf4j)

lazy val benchmarkServer = (project in file("examples/benchmark-server"))
  .settings(baseServerSettings)
  .settings(noPublishSettings)
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

lazy val exampleHttpJavaServer = (project in file("examples/java-http-server"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-http-server",
    moduleName := "java-http-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    injectCore % "test->test",
    injectSlf4j)

lazy val exampleInjectJavaServer = (project in file("examples/java-server"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-server",
    moduleName := "java-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).dependsOn(
    injectServer % "test->test;compile->compile",
    injectCore % "test->test",
    injectApp % "test->test",
    injectSlf4j)

lazy val thriftExampleIdl = (project in file("examples/thrift-server/thrift-example-idl"))
  .settings(baseServerSettings)
  .settings(noPublishSettings)
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
  .settings(noPublishSettings)
  .settings(
    name := "thrift-example-server",
    moduleName := "thrift-example-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ExceptionTranslationFilter.*"
  ).dependsOn(
    thriftExampleIdl,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectSlf4j)

lazy val thriftJavaExampleIdl = (project in file("examples/java-thrift-server/thrift-example-idl"))
  .settings(baseServerSettings)
  .settings(noPublishSettings)
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
  .settings(noPublishSettings)
  .settings(
    name := "java-thrift-example-server",
    moduleName := "java-thrift-example-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    )
  ).dependsOn(
    thriftJavaExampleIdl,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectSlf4j)

lazy val exampleWebDashboard = (project in file("examples/web-dashboard"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "web-dashboard",
    moduleName := "web-dashboard",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % versions.guava
    ),
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src" / "main" / "webapp"
  ).dependsOn(
    http % "test->test;compile->compile",
    httpclient,
    injectCore % "test->test",
    injectSlf4j)

lazy val exampleTwitterServer = (project in file("examples/example-twitter-server"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "example-twitter-server",
    moduleName := "example-twitter-server"
    ).dependsOn(
      injectServer % "test->test;compile->compile",
      injectSlf4j,
      utils)
// END EXAMPLES
