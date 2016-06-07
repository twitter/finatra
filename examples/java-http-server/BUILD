target(name='java-http-server',
  dependencies=[
    'finatra/examples/java-http-server/src/main/java'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/java-http-server/src/test/java'
  ]
)

jvm_binary(
  name='bin',
  basename='java-http-server',
  main='com.twitter.hello.server.HelloWorldServerMain',
  dependencies=[
    ':java-http-server'
  ],
  deploy_excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
