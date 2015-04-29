scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'benchmarks',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    'finatra/benchmarks/src/main/java',
    'finatra-internal/authentication',
    'finatra-internal/server'
  ],
  sources=rglobs('*.scala'),
)
