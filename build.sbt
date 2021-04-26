import scala.language.reflectiveCalls
import scoverage.ScoverageKeys

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

// All Twitter library releases are date versioned as YY.MM.patch
val releaseVersion = "21.5.0-SNAPSHOT"

lazy val buildSettings = Seq(
  version := releaseVersion,
  scalaVersion := "2.12.12",
  crossScalaVersions := Seq("2.12.12", "2.13.1"),
  scalaModuleInfo := scalaModuleInfo.value.map(_.withOverrideScalaVersion(true)),
  fork in Test := true, // We have to fork to get the JavaOptions
  javaOptions in Test ++= travisTestJavaOptions,
  libraryDependencies += scalaCollectionCompat
)

lazy val noPublishSettings = Seq(
  skip in publish := true
)

def gcJavaOptions: Seq[String] = {
  val javaVersion = System.getProperty("java.version")
  if (javaVersion.startsWith("1.8")) {
    jdk8GcJavaOptions
  } else {
    jdk11GcJavaOptions
  }
}

def jdk8GcJavaOptions: Seq[String] = {
  Seq(
    "-XX:+UseParNewGC",
    "-XX:+UseConcMarkSweepGC",
    "-XX:+CMSParallelRemarkEnabled",
    "-XX:+CMSClassUnloadingEnabled",
    "-XX:ReservedCodeCacheSize=128m",
    "-XX:SurvivorRatio=128",
    "-XX:MaxTenuringThreshold=0",
    "-Xss8M",
    "-Xms512M",
    "-Xmx2G"
  )
}

def jdk11GcJavaOptions: Seq[String] = {
  Seq(
    "-XX:+UseConcMarkSweepGC",
    "-XX:+CMSParallelRemarkEnabled",
    "-XX:+CMSClassUnloadingEnabled",
    "-XX:ReservedCodeCacheSize=128m",
    "-XX:SurvivorRatio=128",
    "-XX:MaxTenuringThreshold=0",
    "-Xss8M",
    "-Xms512M",
    "-Xmx2G"
  )
}

def travisTestJavaOptions: Seq[String] = {
  // When building on travis-ci, we want to suppress logging to error level only.
  // https://docs.travis-ci.com/user/environment-variables/#default-environment-variables
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-DSKIP_FLAKY=true",
      "-DSKIP_FLAKY_TRAVIS=true",
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=off",
      "-Dcom.twitter.inject.test.logging.disabled",
      // Needed to avoid cryptic EOFException crashes in forked tests
      // in Travis with `sudo: false`.
      // See https://github.com/sbt/sbt/issues/653
      // and https://github.com/travis-ci/travis-ci/issues/3775
      "-Xmx3G"
    )
  } else {
    Seq("-DSKIP_FLAKY=true")
  }
}

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")

  // All Twitter library releases are date versioned as YY.MM.patch
  val twLibVersion = releaseVersion

  val agrona = "0.9.22"
  val commonsFileupload = "1.4"
  val fastutil = "8.1.1"
  val guice = "4.2.3"
  val jackson = "2.11.2"
  val jodaConvert = "1.5"
  val jodaTime = "2.10.8"
  val json4s = "3.6.7"
  val junit = "4.12"
  val kafka24 = "2.4.1"
  val kafka25 = "2.5.0"
  val libThrift = "0.10.0"
  val logback = "1.2.3"
  val mockitoScala = "1.14.8"
  val mustache = "0.8.18"
  val nscalaTime = "2.22.0"
  val rocksdbjni = "5.14.2"
  val scalaCheck = "1.14.3"
  val scalaGuice = "4.2.11"
  val scalaTest = "3.1.2"
  val scalaTestPlusJunit = "3.1.2.0"
  val scalaTestPlusScalaCheck = "3.1.2.0"
  val servletApi = "2.5"
  val slf4j = "1.7.30"
  val snakeyaml = "1.24"
  val javaxBind = "2.3.0"
  val javaxActivation = "1.1.1"
  val scalaCollectionCompat = "2.1.2"
}

lazy val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2"

lazy val scalaCompilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-Ywarn-unused:imports"
)

lazy val testDependenciesSettings = Seq(
  libraryDependencies ++= Seq(
    "junit" % "junit" % versions.junit, // used by Java tests & org.junit.runner.RunWith annotation on c.t.inject.Test
    "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test,
    "org.scalatest" %% "scalatest" % versions.scalaTest % Test,
    "org.scalatestplus" %% "junit-4-12" % versions.scalaTestPlusJunit % Test,
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalaTestPlusScalaCheck % Test
  )
)

lazy val baseSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalaCompilerOptions,
  javacOptions in (Compile, compile) ++= Seq(
    "-source",
    "1.8",
    "-target",
    "1.8",
    "-Xlint:unchecked"),
  javacOptions in doc ++= Seq("-source", "1.8"),
  javaOptions ++= Seq(
    "-Djava.net.preferIPv4Stack=true",
    "-XX:+AggressiveOpts",
    "-server"
  ),
  javaOptions ++= gcJavaOptions,
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
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
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
  licenses := Seq("Apache 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
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
  pomPostProcess := { node: scala.xml.Node =>
    val rule: scala.xml.transform.RewriteRule = new scala.xml.transform.RewriteRule {
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
    val contents =
      s"name=${name.value}\nversion=${version.value}\nbuild_revision=$buildRev\nbuild_name=$buildName"
    IO.write(file, contents)
    Seq(file)
  }.taskValue
)

lazy val projectSettingNoTestDependencies = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  organization := "com.twitter"
)

lazy val projectSettings = projectSettingNoTestDependencies ++ testDependenciesSettings

lazy val baseServerSettings = baseSettings ++ buildSettings ++ publishSettings ++ Seq(
  organization := "com.twitter",
  publishArtifact := false,
  publishLocal := {},
  publish := {},
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val exampleServerSettings = baseServerSettings ++ Seq(
  fork in run := true,
  javaOptions in Test ++= Seq(
    // we are unable to guarantee that Logback will not get picked up b/c of coursier caching
    // so we set the Logback System properties in addition to the slf4j-simple and the
    // the Framework test logging disabled property.
    "-Dlog.service.output=/dev/stdout",
    "-Dlog.access.output=/dev/stdout",
    "-Dlog_level=OFF",
    "-Dorg.slf4j.simpleLogger.defaultLogLevel=off",
    "-Dcom.twitter.inject.test.logging.disabled"
  ),
  libraryDependencies ++= Seq(
    "com.twitter" %% "twitter-server-logback-classic" % versions.twLibVersion,
    "ch.qos.logback" % "logback-classic" % versions.logback
  ),
  excludeDependencies ++= Seq(
    // commons-logging is replaced by jcl-over-slf4j
    ExclusionRule(organization = "commons-logging", name = "commons-logging")
  )
)

lazy val finatraModules = Seq[sbt.ProjectReference](
  benchmarks,
  httpAnnotations,
  httpClient,
  httpCore,
  httpMustache,
  httpServer,
  injectApp,
  injectCore,
  injectDtab,
  injectLogback,
  injectMdc,
  injectModules,
  injectPorts,
  injectRequestScope,
  injectServer,
  injectSlf4j,
  injectStack,
  injectThrift,
  injectThriftClient,
  injectThriftClientHttpMapper,
  injectUtils,
  jackson,
  jsonAnnotations,
  kafka,
  kafkaStreams,
  kafkaStreamsPrerestore,
  kafkaStreamsQueryableThrift,
  kafkaStreamsQueryableThriftClient,
  kafkaStreamsStaticPartitioning,
  mustache,
  thrift,
  utils,
  validation
)

lazy val finatraExamples =
  // START EXAMPLES
  Seq[sbt.ProjectReference](
    benchmark,
    javaInjectableApp,
    scalaInjectableApp,
    javaInjectableTwitterServer,
    scalaInjectableTwitterServer,
    javaHttpServer,
    scalaHttpServer,
    thriftIdl,
    javaThriftServer,
    scalaThriftServer,
    streamingExample,
    twitterClone,
    exampleWebDashboard
  ) ++
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
      -- inProjects(
        benchmark,
        javaInjectableApp,
        scalaInjectableApp,
        javaInjectableTwitterServer,
        scalaInjectableTwitterServer,
        javaHttpServer,
        scalaHttpServer,
        thriftIdl,
        javaThriftServer,
        scalaThriftServer,
        streamingExample,
        twitterClone,
        exampleWebDashboard
      )
    // END EXAMPLES
  ).aggregate(aggregatedProjects: _*)

lazy val injectCoreTestJarSources =
  Seq(
    "com/twitter/inject/InMemoryStats",
    "com/twitter/inject/InMemoryStatsReceiverUtility",
    "com/twitter/inject/IntegrationTest",
    "com/twitter/inject/IntegrationTestMixin",
    "com/twitter/inject/PoolUtils",
    "com/twitter/inject/Test",
    "com/twitter/inject/TestMixin",
    "com/twitter/inject/WhenReadyMixin"
  )
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
      "javax.inject" % "javax.inject" % "1",
      "joda-time" % "joda-time" % versions.jodaTime,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.joda" % "joda-convert" % versions.jodaConvert,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "com.google.inject" % "guice" % versions.guice % Test,
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % Test,
      "com.twitter" %% "finagle-stats" % versions.twLibVersion % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectSlf4j
  )

lazy val injectStack = (project in file("inject/inject-stack"))
  .settings(projectSettings)
  .settings(
    name := "inject-stack",
    moduleName := "inject-stack",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
    )
  )

lazy val injectLogback = (project in file("inject/inject-logback"))
  .settings(projectSettings)
  .settings(
    name := "inject-logback",
    moduleName := "inject-logback",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "ch.qos.logback" % "logback-core" % versions.logback,
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-registry" % versions.twLibVersion,
      "com.twitter" %% "util-stats" % versions.twLibVersion
    ),
    // we don't want slf4j-simple in test from any dependency (3rdparty or other module)
    excludeDependencies in Test ++= Seq(
      ExclusionRule(organization = "org.slf4j", name = "slf4j-simple")
    )
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    httpServer % "test->test;test->compile"
  )

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
      "com.twitter" %% "util-stats" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectCore % "test->test;compile->compile",
    injectStack
  )

lazy val injectAppTestJarSources =
  Seq(
    "com/twitter/inject/app/Banner",
    "com/twitter/inject/app/BindDSL",
    "com/twitter/inject/app/EmbeddedApp",
    "com/twitter/inject/app/InjectionServiceModule",
    "com/twitter/inject/app/InjectionServiceWithAnnotationModule",
    "com/twitter/inject/app/StartupTimeoutException",
    "com/twitter/inject/app/TestInjector"
  )
lazy val injectApp = (project in file("inject/inject-app"))
  .settings(projectSettings)
  .settings(
    name := "inject-app",
    moduleName := "inject-app",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "com.twitter" %% "finagle-core" % versions.twLibVersion % Test,
      "com.twitter" %% "util-mock" % versions.twLibVersion % Test,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      // -------- BEGIN: slf4j-api logging bridges -------------------------------
      // Add the slf4j-api logging bridges to ensure that any dependents
      // of the library have bridges on their classpath at runtime.
      "org.slf4j" % "jcl-over-slf4j" % versions.slf4j,
      "org.slf4j" % "jul-to-slf4j" % versions.slf4j,
      "org.slf4j" % "log4j-over-slf4j" % versions.slf4j,
      // -------- END: slf4j-api logging bridges ---------------------------------
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectModules % "test->test",
    injectUtils
  )

lazy val injectDtab = (project in file("inject/inject-dtab"))
  .settings(projectSettings)
  .settings(
    name := "inject-dtab",
    moduleName := "inject-dtab",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion
    )
  ).dependsOn(
    injectApp
  )

lazy val injectPorts = (project in file("inject/inject-ports"))
  .settings(projectSettings)
  .settings(
    name := "inject-ports",
    moduleName := "inject-ports",
    ScoverageKeys.coverageExcludedPackages := "<empty>",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "twitter-server" % versions.twLibVersion,
      "com.twitter" %% "util-app" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectCore % "test->test"
  )

lazy val injectServerTestJarSources =
  Seq(
    "com/twitter/inject/server/AdminHttpClient",
    "com/twitter/inject/server/EmbeddedHttpClient",
    "com/twitter/inject/server/EmbeddedTwitterServer",
    "com/twitter/inject/server/FeatureTest",
    "com/twitter/inject/server/FeatureTestMixin",
    "com/twitter/inject/server/package"
  )
lazy val injectServer = (project in file("inject/inject-server"))
  .settings(projectSettings)
  .settings(
    name := "inject-server",
    moduleName := "inject-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>",
    libraryDependencies ++= Seq(
      "com.twitter" %% "twitter-server" % versions.twLibVersion,
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectPorts % "test->test;compile->compile",
    injectSlf4j,
    injectUtils
  )

lazy val injectMdc = (project in file("inject/inject-mdc"))
  .settings(projectSettings)
  .settings(
    name := "inject-mdc",
    moduleName := "inject-mdc",
    ScoverageKeys.coverageExcludedPackages := "<empty>;",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-slf4j-api" % versions.twLibVersion,
      "org.slf4j" % "slf4j-api" % versions.slf4j
    )
  ).dependsOn(
    injectSlf4j,
    injectCore % "test->test"
  )

lazy val injectSlf4j = (project in file("inject/inject-slf4j"))
  .settings(projectSettings)
  .settings(
    name := "inject-slf4j",
    moduleName := "inject-slf4j",
    ScoverageKeys.coverageExcludedPackages := "<empty>;",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-slf4j-api" % versions.twLibVersion,
      "org.slf4j" % "slf4j-api" % versions.slf4j
    )
  )

lazy val injectRequestScope = (project in file("inject/inject-request-scope"))
  .settings(projectSettings)
  .settings(
    name := "inject-request-scope",
    moduleName := "inject-request-scope",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectApp % "test->test"
  )

lazy val injectThrift = (project in file("inject/inject-thrift"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift",
    moduleName := "inject-thrift",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % versions.libThrift exclude ("commons-logging", "commons-logging"),
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-mux" % versions.twLibVersion,
      "com.twitter" %% "scrooge-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-mock" % versions.twLibVersion % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectCore % "test->test",
    injectUtils
  )

lazy val injectThriftClient = (project in file("inject/inject-thrift-client"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift-client",
    moduleName := "inject-thrift-client",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*;.*LatencyFilter.*",
    scroogeLanguages in Test := Seq("java", "scala"),
    scroogePublishThrift in Test := true,
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thrift" % versions.twLibVersion,
      "com.twitter" %% "finagle-thriftmux" % versions.twLibVersion,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.twLibVersion % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectCore,
    injectUtils,
    injectApp % "test->test;compile->compile",
    injectModules % "test->test;compile->compile",
    injectThrift,
    httpServer % "test->test",
    thrift % "test->test",
    utils
  )

lazy val injectUtils = (project in file("inject/inject-utils"))
  .settings(projectSettings)
  .settings(
    name := "inject-utils",
    moduleName := "inject-utils",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "javax.xml.bind" % "jaxb-api" % versions.javaxBind,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectCore % "test->test;compile->compile"
  )

/**
 * This project relies on other projects test dependencies and as such
 * needs to have all of the benchmarks defined in test scope to play
 * well with IDEs other build systems.
 *
 * @see https://github.com/ktoso/sbt-jmh/issues/63
 *
 * To run, open sbt console:
 * {{{
 *   $ ./sbt
 *   > project benchmarks
 *   > jmh:run -i 10 -wi 20 -f1 -t1 .*JsonBenchmark.*
 * }}}
 *
 * Which means "10 iterations" "20 warm up iterations" "1 fork" "1 thread". Note that
 * benchmarks should be usually executed at least in 10 iterations (as a rule of thumb),
 * but more is better.
 *
 * For "real" results the recommendation is to warm up at least 10 to 20 iterations, and then
 * measure 10 to 20 iterations again. Forking the JVM is required to avoid falling into specific
 * optimizations.
 */
lazy val benchmarks = project
  .settings(baseServerSettings)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq("org.slf4j" % "slf4j-simple" % versions.slf4j % Test)
  )
  .settings(noPublishSettings)
  .dependsOn(
    httpMustache % "test",
    httpServer,
    injectRequestScope,
    injectCore % "test->test",
    injectApp % "test->test;compile->compile"
  )

lazy val utilsTestJarSources =
  Seq("com/twitter/finatra/modules/", "com/twitter/finatra/test/")
lazy val utils = project
  .settings(projectSettings)
  .settings(
    name := "finatra-utils",
    moduleName := "finatra-utils",
    ScoverageKeys.coverageExcludedPackages := "<empty>;com\\.twitter\\.finatra\\..*package.*;.*WrappedValue.*;.*DeadlineValues.*;.*RichBuf.*;.*RichByteBuf.*",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % versions.guice,
      "joda-time" % "joda-time" % versions.jodaTime,
      "com.github.nscala-time" %% "nscala-time" % versions.nscalaTime,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "javax.activation" % "activation" % versions.javaxActivation,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectUtils
  )

lazy val validationTestJarSources =
  Seq(
    "com/twitter/finatra/validation/ValidatorTest"
  )
lazy val validation = project
  .settings(projectSettings)
  .settings(
    name := "finatra-validation",
    moduleName := "finatra-validation",
    libraryDependencies ++= Seq(
      "joda-time" % "joda-time" % versions.jodaTime,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "org.json4s" %% "json4s-core" % versions.json4s,
      "com.fasterxml.jackson.core" % "jackson-annotations" % versions.jackson % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    ),
    // special-case to only scaladoc what's necessary as some of the tests cannot generate scaladocs
    sources in Test in doc := {
      val previous: Seq[File] = (sources in Test in doc).value
      previous.filter(file =>
        validationTestJarSources.foldLeft(false)(_ || file.getPath.contains(_)))
    },
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, validationTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, validationTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, validationTestJarSources))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectSlf4j,
    injectUtils,
    utils % "test->test;compile->compile"
  )

lazy val jacksonTestJarSources =
  Seq(
    "com/twitter/finatra/json/JsonDiff"
  )
lazy val jackson = project
  .settings(projectSettings)
  .settings(
    name := "finatra-jackson",
    moduleName := "finatra-jackson",
    ScoverageKeys.coverageExcludedPackages := ".*DurationMillisSerializer.*;.*ByteBufferUtils.*",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
      "com.google.inject" % "guice" % versions.guice,
      "org.json4s" %% "json4s-core" % versions.json4s,
      "com.twitter" %% "finagle-http" % versions.twLibVersion % "test",
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
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
    injectCore,
    injectSlf4j,
    injectUtils,
    jsonAnnotations,
    validation % "test->test;compile->compile",
    utils
  )

lazy val jsonAnnotations = (project in file("json-annotations"))
  .settings(projectSettings)
  .settings(
    name := "finatra-json-annotations",
    moduleName := "finatra-json-annotations",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % versions.guice
    )
  )

lazy val mustache = project
  .settings(projectSettings)
  .settings(
    name := "finatra-mustache",
    moduleName := "finatra-mustache",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ScalaObjectHandler.*",
    libraryDependencies ++= Seq(
      "javax.inject" % "javax.inject" % "1",
      "com.github.spullara.mustache.java" % "compiler" % versions.mustache exclude ("com.google.guava", "guava"),
      "com.google.inject" % "guice" % versions.guice,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectApp % "test->test;compile->compile",
    injectCore % "test->test;compile->compile",
    utils
  )

lazy val httpCore = (project in file("http-core"))
  .settings(projectSettings)
  .settings(
    name := "finatra-http-core",
    moduleName := "finatra-http-core",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "commons-fileupload" % "commons-fileupload" % versions.commonsFileupload,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "javax.servlet" % "servlet-api" % versions.servletApi % "provided;compile->compile;test->test"
    ),
    excludeFilter in Test in unmanagedResources := "BUILD",
    publishArtifact in Test := true
  ).dependsOn(
    httpAnnotations,
    jackson % "test->test;compile->compile",
    utils % "test->test;compile->compile"
  )

lazy val httpTestJarSources =
  Seq(
    "com/twitter/finatra/http/EmbeddedHttpServer",
    "com/twitter/finatra/http/ExternalHttpClient",
    "com/twitter/finatra/http/HttpMockResponses",
    "com/twitter/finatra/http/HttpTest",
    "com/twitter/finatra/http/JsonAwareEmbeddedHttpClient",
    "com/twitter/finatra/http/RouteHint",
    "com/twitter/finatra/http/StreamingJsonTestHelper",
    "com/twitter/finatra/http/modules/ResponseBuilderModule",
    "com/twitter/finatra/http/response/DefaultResponseBuilder"
  )
lazy val httpServer = (project in file("http-server"))
  .settings(projectSettings)
  .settings(
    name := "finatra-http-server",
    moduleName := "finatra-http-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;com\\.twitter\\.finatra\\..*package.*;.*ThriftExceptionMapper.*;.*HttpResponseExceptionMapper.*;.*HttpResponseException.*",
    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % versions.libThrift intransitive (),
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "javax.servlet" % "servlet-api" % versions.servletApi % "provided;compile->compile;test->test",
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal",
      "com.twitter" %% "util-security-test-certs" % versions.twLibVersion % Test
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
    httpAnnotations,
    httpClient % "test->test",
    httpCore,
    injectRequestScope % Test,
    injectPorts % "test->test",
    injectMdc,
    injectSlf4j,
    injectServer % "test->test;compile->compile",
    jackson % "test->test;compile->compile",
    utils % "test->test;compile->compile",
    validation % "test"
  )

lazy val httpAnnotations = (project in file("http-annotations"))
  .settings(projectSettings)
  .settings(
    name := "finatra-http-annotations",
    moduleName := "finatra-http-annotations"
  ).dependsOn(
    jsonAnnotations
  )

lazy val httpMustache = (project in file("http-mustache"))
  .settings(projectSettings)
  .settings(
    name := "finatra-http-mustache",
    moduleName := "finatra-http-mustache",
    libraryDependencies ++= Seq(
      "javax.inject" % "javax.inject" % "1",
      "com.github.spullara.mustache.java" % "compiler" % versions.mustache exclude ("com.google.guava", "guava"),
      "com.google.inject" % "guice" % versions.guice,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    httpAnnotations,
    injectCore % "test->test;compile->compile",
    injectUtils,
    mustache % "test->test;compile->compile",
    utils % "test->test;compile->compile"
  )

lazy val httpClientTestJarSources =
  Seq("com/twitter/finatra/httpclient/test/")
lazy val httpClient = (project in file("http-client"))
  .settings(projectSettings)
  .settings(
    name := "finatra-http-client",
    moduleName := "finatra-http-client",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-http" % versions.twLibVersion,
      "com.twitter" %% "twitter-server" % versions.twLibVersion % Test,
      "com.twitter" %% "util-core" % versions.twLibVersion,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    ),
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, httpClientTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, httpClientTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, httpClientTestJarSources))
    }
  ).dependsOn(
    httpCore,
    jackson,
    injectModules,
    injectUtils,
    injectApp % "test->test",
    injectCore % "test->test"
  )

lazy val thriftTestJarSources =
  Seq(
    "com/twitter/finatra/thrift/EmbeddedThriftServer",
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
      "com.twitter" %% "util-reflect" % versions.twLibVersion,
      "javax.inject" % "javax.inject" % "1",
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.yaml" % "snakeyaml" % versions.snakeyaml,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    ),
    scroogePublishThrift in Test := true,
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
    injectPorts % "test->test",
    injectServer % "test->test;compile->compile",
    injectMdc,
    injectSlf4j,
    injectStack,
    injectThrift
  )

lazy val injectThriftClientHttpMapper = (project in file("inject-thrift-client-http-mapper"))
  .settings(projectSettings)
  .settings(
    name := "inject-thrift-client-http-mapper",
    moduleName := "inject-thrift-client-http-mapper",
    excludeFilter in Test in unmanagedResources := "BUILD",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    injectCore,
    injectServer % "test->test",
    injectSlf4j % "test->test",
    injectThriftClient % "test->test;compile->compile",
    thrift % "test->test;test->compile"
  )

lazy val kafkaStreamsExclusionRules = Seq(
  ExclusionRule(organization = "javax.ws.rs", name = "javax.ws.rs-api"),
  ExclusionRule(organization = "log4j", name = "log4j"),
  ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12")
)

lazy val kafkaTestJarSources =
  Seq(
    "com/twitter/finatra/kafka/test/EmbeddedKafka",
    "com/twitter/finatra/kafka/test/KafkaTopic",
    "com/twitter/finatra/kafka/test/utils/ThreadUtils",
    "com/twitter/finatra/kafka/test/utils/PollUtils",
    "com/twitter/finatra/kafka/test/utils/InMemoryStatsUtil",
    "com/twitter/finatra/kafka/test/KafkaFeatureTest",
    "com/twitter/finatra/kafka/test/KafkaStateStore"
  )

def kafkaDependencies(kafkaVersion: String): Seq[ModuleID] = {
  Seq(
    "org.apache.kafka" %% "kafka" % kafkaVersion % "compile->compile;test->test",
    "org.apache.kafka" %% "kafka" % kafkaVersion % "test" classifier "test",
    "org.apache.kafka" % "kafka-clients" % kafkaVersion % "test->test",
    "org.apache.kafka" % "kafka-clients" % kafkaVersion % "test" classifier "test",
    "org.apache.kafka" % "kafka-streams" % kafkaVersion % "compile->compile;test->test",
    "org.apache.kafka" % "kafka-streams" % kafkaVersion % "test" classifier "test",
    "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion % "compile->compile;test->test",
    "org.apache.kafka" % "kafka-streams-test-utils" % kafkaVersion % "test" classifier "test"
  )
}

def kafkaStreamsDependencies(kafkaVersion: String): Seq[ModuleID] = {
  Seq(
    "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion % "compile->compile;test->test",
    "org.apache.kafka" % "kafka-streams" % kafkaVersion % "compile->compile;test->test",
    "org.apache.kafka" % "kafka-streams" % kafkaVersion % "test" classifier "test"
  )
}

// select the library and different set of source files with different scala version
def crossVersionKafka(
  scalaVersion: Option[(Long, Long)],
  kafkaWithscala212: String,
  kafkaWithscala213: String
): String = {
  scalaVersion match {
    case Some((2, n)) if n <= 12 => kafkaWithscala212
    case _ => kafkaWithscala213
  }
}

lazy val kafka = (project in file("kafka"))
  .settings(projectSettings)
  .settings(
    name := "finatra-kafka",
    moduleName := "finatra-kafka",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % versions.twLibVersion,
      "com.twitter" %% "finagle-exp" % versions.twLibVersion,
      "com.twitter" %% "finagle-thrift" % versions.twLibVersion,
      "com.twitter" %% "scrooge-serializer" % versions.twLibVersion,
      "com.twitter" %% "util-codec" % versions.twLibVersion,
      "org.slf4j" % "slf4j-api" % versions.slf4j % "compile->compile;test->test",
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    ),
    libraryDependencies ++= {
      val scalaV = CrossVersion.partialVersion(scalaVersion.value)
      kafkaDependencies(crossVersionKafka(scalaV, versions.kafka24, versions.kafka25))
    },
    excludeDependencies in Test ++= kafkaStreamsExclusionRules,
    excludeDependencies ++= kafkaStreamsExclusionRules,
    scroogeThriftIncludeFolders in Test := Seq(file("src/test/thrift")),
    scroogeLanguages in Test := Seq("scala"),
    excludeFilter in unmanagedResources := "BUILD",
    publishArtifact in Test := true,
    mappings in (Test, packageBin) := {
      val previous = (mappings in (Test, packageBin)).value
      previous.filter(mappingContainsAnyPath(_, kafkaTestJarSources))
    },
    mappings in (Test, packageDoc) := {
      val previous = (mappings in (Test, packageDoc)).value
      previous.filter(mappingContainsAnyPath(_, kafkaTestJarSources))
    },
    mappings in (Test, packageSrc) := {
      val previous = (mappings in (Test, packageSrc)).value
      previous.filter(mappingContainsAnyPath(_, kafkaTestJarSources))
    }
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectSlf4j % "test->test;compile->compile",
    injectUtils % "test->test;compile->compile",
    jackson % "test->test",
    utils % "test->test;compile->compile"
  )

lazy val kafkaStreamsQueryableThriftClient =
  (project in file("kafka-streams/kafka-streams-queryable-thrift-client"))
    .settings(projectSettings)
    .settings(
      name := "finatra-kafka-streams-queryable-thrift-client",
      moduleName := "finatra-kafka-streams-queryable-thrift-client",
      ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-serversets" % versions.twLibVersion,
        "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
      ),
      excludeDependencies in Test ++= kafkaStreamsExclusionRules,
      excludeDependencies ++= kafkaStreamsExclusionRules,
      excludeFilter in unmanagedResources := "BUILD"
    ).dependsOn(
      injectCore % "test->test;compile->compile",
      injectSlf4j % "test->test;compile->compile",
      injectUtils % "test->test;compile->compile",
      thrift % "test->test;compile->compile",
      utils % "test->test;compile->compile"
    )

lazy val kafkaStreamsStaticPartitioning =
  (project in file("kafka-streams/kafka-streams-static-partitioning"))
    .settings(projectSettings)
    .settings(
      name := "finatra-kafka-streams-static-partitioning",
      moduleName := "finatra-kafka-streams-static-partitioning",
      ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
      unmanagedSources / includeFilter in Compile := {
        val scalaV = CrossVersion.partialVersion(scalaVersion.value)
        scalaV match {
          case _ => "*.scala"
        }
      },
      Compile / unmanagedSourceDirectories += {
        val sourceDir = (Compile / sourceDirectory).value
        sourceDir / "scala-kafka2.5"
      },
      excludeDependencies in Test ++= kafkaStreamsExclusionRules,
      excludeDependencies ++= kafkaStreamsExclusionRules,
      excludeFilter in unmanagedResources := "BUILD",
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
      )
    ).dependsOn(
      injectCore % "test->test;compile->compile",
      injectSlf4j % "test->test;compile->compile",
      injectUtils % "test->test;compile->compile",
      kafkaStreams % "test->test;compile->compile",
      kafkaStreamsQueryableThriftClient % "test->test;compile->compile",
      thrift % "test->test;compile->compile",
      utils % "test->test;compile->compile"
    )

lazy val kafkaStreamsPrerestore = (project in file("kafka-streams/kafka-streams-prerestore"))
  .settings(projectSettings)
  .settings(
    name := "finatra-kafka-streams-prerestore",
    moduleName := "finatra-kafka-streams-prerestore",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
    excludeDependencies in Test ++= kafkaStreamsExclusionRules,
    excludeDependencies ++= kafkaStreamsExclusionRules,
    excludeFilter in unmanagedResources := "BUILD",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test",
    kafkaStreamsStaticPartitioning % "test->test;compile->compile"
  )

lazy val kafkaStreamsQueryableThrift =
  (project in file("kafka-streams/kafka-streams-queryable-thrift"))
    .settings(projectSettings)
    .settings(
      name := "finatra-kafka-streams-queryable-thrift",
      moduleName := "finatra-kafka-streams-queryable-thrift",
      ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
      excludeDependencies in Test ++= kafkaStreamsExclusionRules,
      excludeDependencies ++= kafkaStreamsExclusionRules,
      scroogeThriftIncludeFolders in Compile := Seq(file("src/test/thrift")),
      scroogeLanguages in Compile := Seq("java", "scala"),
      scroogeLanguages in Test := Seq("java", "scala"),
      excludeFilter in unmanagedResources := "BUILD",
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
      )
    ).dependsOn(
      injectCore % "test->test;compile->compile",
      kafkaStreamsStaticPartitioning % "test->test;compile->compile"
    )

lazy val kafkaStreams = (project in file("kafka-streams/kafka-streams"))
  .settings(projectSettings)
  .settings(
    name := "finatra-kafka-streams",
    moduleName := "finatra-kafka-streams",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*",
    unmanagedSourceDirectories in Compile += {
      val sourceDir = (sourceDirectory in Compile).value
      val scalaV = CrossVersion.partialVersion(scalaVersion.value)
      sourceDir / "scala-kafka2.5"
    },
    unmanagedSourceDirectories in Test += {
      val testDir = (sourceDirectory in Test).value
      val scalaV = CrossVersion.partialVersion(scalaVersion.value)
      testDir / crossVersionKafka(scalaV, "scala-kafka2.4", "scala-kafka2.5")
    },
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-jvm" % versions.twLibVersion,
      "it.unimi.dsi" % "fastutil" % versions.fastutil,
      "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.3",
      "org.agrona" % "agrona" % versions.agrona,
      "org.rocksdb" % "rocksdbjni" % versions.rocksdbjni % "provided;compile->compile;test->test",
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    ),
    libraryDependencies ++= {
      val scalaV = CrossVersion.partialVersion(scalaVersion.value)
      kafkaStreamsDependencies(crossVersionKafka(scalaV, versions.kafka24, versions.kafka25))
    },
    excludeDependencies in Test ++= kafkaStreamsExclusionRules,
    excludeDependencies ++= kafkaStreamsExclusionRules,
    excludeFilter in unmanagedResources := "BUILD",
    publishArtifact in Test := true
  ).dependsOn(
    injectCore % "test->test;compile->compile",
    injectSlf4j % "test->test;compile->compile",
    injectUtils % "test->test;compile->compile",
    jackson % "test->test;compile->compile",
    kafka % "test->test;compile->compile",
    kafkaStreamsQueryableThriftClient % "test->test;compile->compile",
    thrift % "test->test",
    utils % "test->test;compile->compile"
  )

lazy val site = (project in file("doc"))
  .enablePlugins(SphinxPlugin)
  .settings(baseSettings ++ buildSettings ++ Seq(
    scalacOptions in doc ++= Seq("-doc-title", "Finatra", "-doc-version", version.value),
    includeFilter in Sphinx := ("*.html" | "*.png" | "*.svg" | "*.js" | "*.css" | "*.gif" | "*.txt")
  ))

// START EXAMPLES

// benchmark

lazy val benchmark = (project in file("examples/benchmark"))
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
    httpServer % "test->test;compile->compile",
    injectCore % "test->test"
  )

// injectable-app

lazy val javaInjectableApp = (project in file("examples/injectable-app/java"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-app",
    moduleName := "java-app",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectApp % "test->test;compile->compile",
    injectLogback,
    injectModules,
    injectSlf4j
  )

lazy val scalaInjectableApp = (project in file("examples/injectable-app/scala"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "scala-app",
    moduleName := "scala-app",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    injectApp % "test->test;compile->compile",
    injectLogback,
    injectModules,
    injectSlf4j
  )

// injectable-twitter-server

lazy val javaInjectableTwitterServer = (project in file("examples/injectable-twitter-server/java"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-twitter-server",
    moduleName := "java-twitter-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal",
      "com.twitter" %% "util-core" % versions.twLibVersion
    )
  ).dependsOn(
    injectServer % "test->test;compile->compile",
    injectCore % "test->test",
    injectApp % "test->test",
    injectSlf4j,
    utils
  )

lazy val scalaInjectableTwitterServer =
  (project in file("examples/injectable-twitter-server/scala"))
    .settings(exampleServerSettings)
    .settings(noPublishSettings)
    .settings(
      name := "scala-twitter-server",
      moduleName := "scala-twitter-server",
      libraryDependencies ++= Seq(
        "com.twitter" %% "util-mock" % versions.twLibVersion % Test,
        "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
      )
    ).dependsOn(
      injectServer % "test->test;compile->compile",
      injectSlf4j,
      utils
    )

// http server

lazy val javaHttpServer = (project in file("examples/http/java"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-http-server",
    moduleName := "java-http-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    httpClient,
    injectCore % "test->test",
    injectSlf4j,
    injectLogback
  )

lazy val scalaHttpServer = (project in file("examples/http/scala"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "scala-http-server",
    moduleName := "scala-http-server",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    injectCore % "test->test",
    injectSlf4j,
    injectLogback
  )

// thrift server

lazy val thriftIdl = (project in file("examples/thrift/idl"))
  .settings(baseServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "thrift-server-idl",
    moduleName := "thrift-example-idl",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*\\.thriftscala.*;.*\\.thriftjava.*",
    scroogeThriftIncludeFolders in Compile := Seq(file("examples/thrift/idl/src/main/thrift"))
  ).dependsOn(
    thrift
  )

lazy val javaThriftServer = (project in file("examples/thrift/java"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "java-thrift-server",
    moduleName := "java-thrift-server",
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    thriftIdl,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectSlf4j,
    injectLogback
  )

lazy val scalaThriftServer = (project in file("examples/thrift/scala"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "scala-thrift-server",
    moduleName := "scala-thrift-server",
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*ExceptionTranslationFilter.*",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    thriftIdl,
    thrift % "test->test;compile->compile",
    injectApp % "test->test",
    injectCore % "test->test",
    injectServer % "test->test",
    injectSlf4j,
    injectLogback
  )

// advanced examples

lazy val streamingExample = (project in file("examples/advanced/streaming-example"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "streaming-example",
    moduleName := "streaming-example",
    libraryDependencies ++= Seq(
      "com.twitter" % "joauth" % "6.0.2",
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    injectCore % "test->test",
    injectSlf4j,
    injectLogback
  )

lazy val twitterClone = (project in file("examples/advanced/twitter-clone"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "twitter-clone",
    moduleName := "twitter-clone",
    ScoverageKeys.coverageExcludedPackages := "<empty>;finatra\\.quickstart\\..*",
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-mock" % versions.twLibVersion % Test,
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    httpClient,
    injectCore % "test->test",
    injectDtab,
    injectSlf4j,
    injectLogback,
    validation
  )

lazy val exampleWebDashboard = (project in file("examples/advanced/web-dashboard"))
  .settings(exampleServerSettings)
  .settings(noPublishSettings)
  .settings(
    name := "web-dashboard",
    moduleName := "web-dashboard",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src" / "main" / "webapp",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % versions.slf4j % "test-internal"
    )
  ).dependsOn(
    httpServer % "test->test;compile->compile",
    httpMustache,
    httpClient,
    injectCore % "test->test",
    injectSlf4j,
    injectLogback,
    mustache
  )
// END EXAMPLES
