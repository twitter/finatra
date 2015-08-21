scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'httpclient',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/io/netty',
    'finagle/finagle-core',
    'finagle/finagle-httpx',
    'finatra/jackson',
    'finatra/utils',
    'util/util-core',
  ],
  sources=rglobs('*.scala')
)
