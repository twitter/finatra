scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-benchmarks',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    'finatra/finatra/finatra-benchmarks/src/main/java',
    'finatra/macaw/macaw-authentication',
    'finatra/macaw/macaw-server'
  ],
  sources=rglobs('*.scala'),
)
