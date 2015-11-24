target(name='java-server',
  dependencies=[
    'finatra/examples/java-server/src/main/java'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/java-server/src/test/java'
  ]
)

jvm_binary(
  name='bin',
  basename='java-server',
  main='com.twitter.hello.HelloWorldServerMain',
  dependencies=[
    ':java-server'
  ],
  deploy_excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
