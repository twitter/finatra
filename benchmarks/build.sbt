import scala.language.reflectiveCalls

lazy val versions = new {
  val slf4j = "1.7.21"
}

name := "finatra-benchmarks"

moduleName := "finatra-benchmarks"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % versions.slf4j
)

excludeDependencies += "org.slf4j" % "jcl-over-slf4j"
excludeDependencies += "org.slf4j" % "jul-to-slf4j"
excludeDependencies += "org.slf4j" % "log4j-over-slf4j"

sourceDirectory in Jmh := (sourceDirectory in Test).value
classDirectory in Jmh := (classDirectory in Test).value
dependencyClasspath in Jmh := (dependencyClasspath in Test).value

// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
compile in Jmh := ((compile in Jmh) dependsOn (compile in Test)).value
run in Jmh := ((run in Jmh) dependsOn (Keys.compile in Jmh)).evaluated
