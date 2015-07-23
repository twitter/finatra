maven_layout()

target(name='finatra-petstore',
  dependencies=[
    'finatra/examples/finatra-petstore/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/finatra-petstore/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-petstore',
  main='com.twitter.hello.PetstoreServerMain',
  dependencies=[
    ':finatra-petstore'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
