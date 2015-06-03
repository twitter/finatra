maven_layout()

target(name='finatra-activator-seed',
  dependencies=[
    'finatra/examples/finatra-activator-seed/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/finatra-activator-seed/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-activator-seed',
  main='com.twitter.example.ExampleServerMain',
  dependencies=[
    ':finatra-activator-seed'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
