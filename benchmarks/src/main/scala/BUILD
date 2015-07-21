scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'benchmarks',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'finagle/finagle-stats',
    'finatra/benchmarks/src/main/java',
    'finatra/http',
    'twitter-server',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
)
