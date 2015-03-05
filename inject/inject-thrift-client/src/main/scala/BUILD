scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-thrift-client',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finagle/finagle-core',
    'finagle/finagle-thrift',
    'finagle/finagle-thriftmux',
    'finatra/inject/inject-core',
    'scrooge/scrooge-core',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
)
