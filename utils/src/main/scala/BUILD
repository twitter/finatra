scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/commons-io',
    '3rdparty/jvm/commons-lang',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/inject/inject-utils',
    'util/util-core',
    'twitter-server',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/utils/src/main/java',
  ],
)
