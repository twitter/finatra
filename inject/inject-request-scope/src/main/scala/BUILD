scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-request-scope',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finatra/inject/inject-core',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/inject/inject-request-scope/src/main/java',
  ],
)
