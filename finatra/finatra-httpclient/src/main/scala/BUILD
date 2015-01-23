scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-httpclient',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/joda-time',
    '3rdparty/jvm/io/netty',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-utils',
    'util/util-core',
  ],
  sources=rglobs('*.scala')
)
