scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/commons-io',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    'finagle/finagle-http',
    'finatra/inject/inject-utils',
    'util/util-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/utils/src/main/java',
  ],
)
