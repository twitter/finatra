target(name='benchmark-server',
  dependencies=[
    'finatra/examples/benchmark-server/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/benchmark-server/src/test/scala'
  ]
)

jvm_binary(
  name='finatra-bin',
  main='com.twitter.benchmark.FinatraBenchmarkServerMain',
  dependencies=[
    ':benchmark-server'
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
    ':benchmark-server'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
