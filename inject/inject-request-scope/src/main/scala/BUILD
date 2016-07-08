scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-request-scope',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finagle/finagle-core',
    'finatra/inject/inject-core',
    'util/util-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/inject/inject-request-scope/src/main/java',
  ],
)
