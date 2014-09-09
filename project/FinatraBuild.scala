import FinatraDependencies._
import sbt.Keys._
import sbt._

object FinatraBuild extends Build {

  override lazy val settings = super.settings ++ Seq(

    /***********************/
    /*** Common Settings ***/
    /***********************/

    logLevel in ThisBuild := Level.Info,

    javacOptions in ThisBuild ++= Seq(
      "-source", "1.7", "-target", "1.7"),

    scalacOptions in ThisBuild ++= Seq(
      "-unchecked", "-deprecation"),

    fork in (Test, run) in ThisBuild := true,

    /*****************/
    /*** Resolvers ***/
    /*****************/

    resolvers ++= Seq(
      Resolver.mavenLocal,
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"),

    /***************/
    /*** Filters ***/
    /***************/

    excludeFilter in unmanagedSources in ThisBuild := HiddenFileFilter || "BUILD",
    excludeFilter in unmanagedResources in ThisBuild := HiddenFileFilter || "BUILD",

    /***************************/
    /*** Common Dependencies ***/
    /***************************/

    libraryDependencies <+= scalaVersion(grizzled_slf4j),
    libraryDependencies <+= scalaVersion(scalatest),
    libraryDependencies <+= scalaVersion(specs2),

    libraryDependencies in ThisBuild ++= Seq(
      junit,
      logback_classic % "test",
      mockito_all,
      slf4j_api))
}
