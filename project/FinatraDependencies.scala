import sbt._

object FinatraDependencies {

  val Version_2_9 = "2.9.2"

  val Version_2_10 = "2.10.4"

  /********************/
  /*** Dependencies ***/
  /********************/

  val commons_codec = "commons-codec" % "commons-codec" % "1.7"
  val commons_fileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"
  val commons_lang = "commons-lang" % "commons-lang" % "2.6"
  val commons_io = "commons-io" % "commons-io" % "2.4"
  val finagle_core = "com.twitter" %% "finagle-core" % "6.18.0"
  val finagle_http = "com.twitter" %% "finagle-http" % "6.18.0"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "2.0.1"
  val guice = "com.google.inject" %  "guice" % "3.0"
  val guice_assistedinject = "com.google.inject.extensions" % "guice-assistedinject" % "3.0"
  val guava = "com.google.guava" % "guava" % "16.0.1"
  val jackson_annotations = "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.2"
  val jackson_core = "com.fasterxml.jackson.core" % "jackson-core" % "2.2.2"
  val jackson_databind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"
  val jackson_datatype_joda = "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.2.2"
  val jackson_module_scala = ("com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.3").
    excludeAll(ExclusionRule(organization = "com.fasterxml.jackson.core"))
  val javax_inject = "javax.inject" % "javax.inject" % "1"
  val javax_servlet_api = "javax.servlet" % "servlet-api" % "2.5"
  val jcl_over_slf4j = "org.slf4j" % "jcl-over-slf4j" % "1.6.1"
  val jmh_core = "org.openjdk.jmh" % "jmh-core" % "0.9"
  val joda_time = "joda-time" %  "joda-time" % "2.2"
  val jul_over_slf4j = "org.slf4j" % "jul-to-slf4j" % "1.6.1"
  val libthrift = "org.apache.thrift" % "libthrift" % "0.5.0"
  val log4j_over_slf4j = "org.slf4j" % "log4j-over-slf4j" % "1.6.1"
  val logback_classic = "ch.qos.logback" % "logback-classic" % "1.0.12"
  val mustache_compiler = "com.github.spullara.mustache.java" % "compiler" % "0.8.12.1"
  val netty = "io.netty" % "netty" % "3.10.0.Final"
  val scala_guice = "net.codingwell" %% "scala-guice" % "3.0.2"
  val slf4j_api = "org.slf4j" % "slf4j-api" % "1.7.7"
  val swagger_annotations = "com.wordnik" % "swagger-annotations" % "1.3.1"
  val twitter_server = "com.twitter" %% "twitter-server" % "1.7.2"
  val util_app = "com.twitter" %% "util-app" % "6.18.0"
  val util_core = "com.twitter" %% "util-core" % "6.18.0"
  val util_logging = "com.twitter" %% "util-logging" % "6.18.0"

  /*************************/
  /*** Test Dependencies ***/
  /*************************/

  val junit = "junit" % "junit" % "4.11" % "test"
  val mockito_all = "org.mockito" % "mockito-all" % "1.9.5" % "test"

  /*************************************/
  /*** Version-specific Dependencies ***/
  /*************************************/

  def grizzled_slf4j(scalaVersion: String) = scalaVersion match {
    case Version_2_9  => "org.clapper"  % "grizzled-slf4j_2.9.2"  % "0.6.10"
    case Version_2_10 => "org.clapper"  % "grizzled-slf4j_2.10"   % "1.0.2"
  }

  def scalaj_time(scalaVersion: String) = scalaVersion match {
    case Version_2_9  => "org.scalaj"  % "scalaj-time_2.9.2"  % "0.7"
    case Version_2_10 => "org.scalaj"  % "scalaj-time_2.10.2" % "0.7"
  }

  def scalap(scalaVersion: String) = scalaVersion match {
    case _  => "org.scala-lang" % "scalap" % scalaVersion
  }

  def scalatest(scalaVersion: String) = scalaVersion match {
    case Version_2_9  => "org.scalatest" % "scalatest_2.9.2"  % "1.9.2" % "test"
    case Version_2_10 => "org.scalatest" % "scalatest_2.10"   % "2.2.0" % "test"
  }

  def specs2(scalaVersion: String) = scalaVersion match {
    case Version_2_9  => "org.specs2" % "specs2_2.9.2" % "1.12.4.1" % "test"
    case Version_2_10 => "org.specs2" % "specs2_2.10"  % "2.3.13" % "test"
  }
}
