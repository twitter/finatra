scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-httpclient',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:guice',
    '3rdparty/jvm/joda-time',
    '3rdparty:netty',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-utils',
    'util/util-core'
  ],
  sources=rglobs('*.scala')
)
