target(name='thrift-example-server',
  dependencies=[
    'finatra/examples/java-thrift-server/thrift-example-server/src/main/java',
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/java-thrift-server/thrift-example-server/src/test/java'
  ]
)

jvm_binary(
  name='bin',
  basename='thrift-example-server',
  main='com.twitter.calculator.CalculatorServerMain',
  dependencies=[
    ':thrift-example-server',
  ],
  deploy_excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)