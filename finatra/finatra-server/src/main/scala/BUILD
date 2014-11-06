scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-server',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:commons-fileupload',
    '3rdparty:commons-io',
    '3rdparty:grizzled-slf4j',
    '3rdparty:guava',
    '3rdparty:guice',
    '3rdparty:jackson-core',
    '3rdparty:jackson-databind',
    '3rdparty:javax.inject',
    '3rdparty:javax-servlet',
    '3rdparty:mustache-compiler',
    '3rdparty:netty',
    '3rdparty/jvm/org/scalaj:scalaj-time',
    '3rdparty:scala-guice',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finagle/finagle-stats',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-jackson/src/main/java',
    'finatra/finatra/finatra-utils',
    'twitter-server',
    'util/util-app',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-server/src/main/resources'
  ]
)
