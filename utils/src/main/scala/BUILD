scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson:jackson-annotations',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/commons-lang:commons-lang',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-core/src/main/scala',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/scala',
    'util/util-app/src/main/scala',
    'util/util-core/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/utils/src/main/java',
  ],
  exports=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/net/codingwell:scala-guice',
  ]

)
