name := "finatra-benchmarks"

moduleName := "finatra-benchmarks"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.21"
)

sourceDirectory in Jmh := (sourceDirectory in Test).value
classDirectory in Jmh := (classDirectory in Test).value
dependencyClasspath in Jmh := (dependencyClasspath in Test).value

// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
compile in Jmh <<= (compile in Jmh) dependsOn (compile in Test)
run in Jmh <<= (run in Jmh) dependsOn (Keys.compile in Jmh)