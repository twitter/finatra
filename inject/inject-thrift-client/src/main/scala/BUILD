scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-thrift-client',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    'finagle/finagle-exp',
    'finagle/finagle-thrift',
    'finagle/finagle-thriftmux',
    'finatra/inject/inject-app',
    'finatra/inject/inject-utils',
    'finatra/inject/inject-thrift',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'finatra/inject/inject-core/src/main/scala:scala',
    'scrooge/scrooge-core/src/main/scala:scala',
    'util/util-app/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
    'util/util-stats/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/inject/inject-thrift-client/src/main/java'
  ],
)
