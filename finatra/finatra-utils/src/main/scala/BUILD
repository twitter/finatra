scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations',
    '3rdparty/jvm/org/scalaj:scalaj-time',
    '3rdparty:commons-io',
    '3rdparty:grizzled-slf4j',
    '3rdparty:guice',
    '3rdparty:guice-assistedinject',
    '3rdparty:libthrift-0.5.0',
    '3rdparty:netty',
    '3rdparty:scala-guice',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-utils/src/main/java',
    'scrooge/scrooge-core',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
)
