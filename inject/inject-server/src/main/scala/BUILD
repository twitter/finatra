scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-server',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-stats',
    'finatra/inject/inject-app',
    'finatra/inject/inject-core',
    'finatra/inject/inject-modules',
    'finatra/inject/inject-slf4j',
    'finatra/inject/inject-utils',
    'twitter-server',
    'util/util-app',
    'util/util-stats',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
    'util/util-logging/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-server/src/main/resources'
  ]
)
