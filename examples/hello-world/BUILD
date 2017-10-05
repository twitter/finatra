target(
  dependencies=[
    'finatra/examples/hello-world/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/hello-world/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-hello-world',
  main='com.twitter.hello.HelloWorldServerMain',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    ':hello-world'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
