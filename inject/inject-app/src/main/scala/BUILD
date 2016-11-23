scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-app',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/org/joda:joda-convert',
    'finatra/inject/inject-core',
    'util/util-app',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/inject/inject-app/src/main/java',
  ],
)
