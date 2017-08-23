scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-jackson',
    repo = artifactory,
  ),
  dependencies=[
    '//:scala-reflect',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-core',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-joda',
    '3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    '3rdparty/jvm/com/google/code/findbugs:jsr305',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/commons-lang',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/joda-time',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/scala-lang:scala-compiler',
    '3rdparty/jvm/org/scala-lang:scalap',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-http/src/main/scala',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/scala',
    'finatra/jackson/src/main/resources',
    'util/util-app/src/main/scala:scala',
    'util/util-core/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/jackson/src/main/java'
  ],
  strict_deps=True,
  exports=[
    '3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
  ]

)
