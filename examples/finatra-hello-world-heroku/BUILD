maven_layout()

target(name='finatra-hello-world-heroku',
  dependencies=[
    'finatra/examples/finatra-hello-world-heroku/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/finatra-hello-world-heroku/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-hello-world-heroku',
  main='com.twitter.hello.HelloWorldServerMain',
  dependencies=[
    ':finatra-hello-world-heroku'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
