scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-request-scope',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/inject/inject-core',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/inject/inject-request-scope/src/main/java',
  ],
)
