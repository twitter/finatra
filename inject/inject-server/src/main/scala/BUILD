scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-server',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-stats/src/main/scala:scala',
    'finatra/inject/inject-app/src/main/scala:scala',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/inject/inject-modules/src/main/scala:scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/scala:scala',
    'twitter-server/src/main/scala:scala',
    'util/util-app/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
    'util/util-logging/src/main/scala:scala',
    'util/util-stats/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-server/src/main/resources'
  ]
)
