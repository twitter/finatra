scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-slf4j',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    '3rdparty/jvm/org/slf4j:jul-to-slf4j',
    '3rdparty/jvm/org/slf4j:jcl-over-slf4j',
    '3rdparty/jvm/org/slf4j:log4j-over-slf4j',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finatra/inject/inject-core',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala')
)
