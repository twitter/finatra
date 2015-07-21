maven_layout()

target(name='finatra-hello-world',
  dependencies=[
    'examples/finatra-hello-world/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'examples/finatra-hello-world/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-hello-world',
  main='com.twitter.hello.HelloWorldServerMain',
  dependencies=[
    ':finatra-hello-world'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
