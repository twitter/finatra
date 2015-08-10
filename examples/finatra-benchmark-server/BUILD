maven_layout()

target(name='finatra-benchmark-server',
  dependencies=[
    'finatra/examples/finatra-benchmark-server/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/finatra-benchmark-server/src/test/scala'
  ]
)

jvm_binary(
  name='finatra-bin',
  main='com.twitter.benchmark.FinatraBenchmarkServerMain',
  dependencies=[
    ':finatra-benchmark-server'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)

jvm_binary(
  name='finagle-bin',
  main='com.twitter.benchmark.FinagleBenchmarkServerMain',
  dependencies=[
    ':finatra-benchmark-server'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
