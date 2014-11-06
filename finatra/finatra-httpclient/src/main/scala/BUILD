scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-httpclient',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:commons-io',
    '3rdparty/jvm/com/twitter/bijection:core',
    '3rdparty:grizzled-slf4j_2.10',
    '3rdparty:guava',
    '3rdparty:guice',
    '3rdparty:guice-assistedinject',
    '3rdparty/jvm/org/scalaj:scalaj-time',
    '3rdparty:libthrift-0.5.0',
    '3rdparty:netty',
    '3rdparty:scala-guice',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-utils',
    'twitter-config/yaml',
    'util/util-core'
  ],
  sources=rglobs('*.scala')
)
