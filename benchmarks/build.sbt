import scala.language.reflectiveCalls

lazy val versions = new {
  val slf4j = "1.7.30"
}

name := "finatra-benchmarks"

moduleName := "finatra-benchmarks"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % versions.slf4j
)

excludeDependencies += "org.slf4j" % "jcl-over-slf4j"
excludeDependencies += "org.slf4j" % "jul-to-slf4j"
excludeDependencies += "org.slf4j" % "log4j-over-slf4j"

Jmh / sourceDirectory  := (Test / sourceDirectory).value
Jmh / classDirectory := (Test / classDirectory).value
Jmh / dependencyClasspath := (Test / dependencyClasspath).value

// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
Jmh / compile := ((Jmh / compile) dependsOn (Test / compile )).value
Jmh / run := ((Jmh / run) dependsOn (Jmh / Keys.compile)).evaluated
