scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-scopes',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-scopes/src/main/java',
    'finatra/finatra/finatra-utils',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
)
