scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-httpclient',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finatra/jackson',
    'finatra/utils',
    'twitter-server',
    '3rdparty/jvm/com/google/guava:guava',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/commons-io:commons-io',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-http/src/main/scala:scala',
    'finatra/inject/inject-core/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala')
)
