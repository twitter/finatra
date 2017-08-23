scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson:jackson-annotations',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    '3rdparty/jvm/commons-lang',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-core/src/main/scala',
    'finagle/finagle-mux/src/main/scala',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/resources',
    'util/util-core/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  strict_deps=True,
  exports=[
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
  ]

)
