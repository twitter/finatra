scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-scopes',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    '3rdparty:javax.inject',
    '3rdparty:jsr305',
    '3rdparty:grizzled-slf4j',
    '3rdparty:guice',
    '3rdparty:scala-guice',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-server',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/finatra/finatra-scopes/src/main/java'
  ]
)
