scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-benchmarks',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    'finatra/finatra/finatra-http'
  ],
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/finatra/finatra-benchmarks/src/main/java'
  ]
)
