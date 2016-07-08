target(name='hello-world-heroku',
  dependencies=[
    'finatra/examples/hello-world-heroku/src/main/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-hello-world-heroku',
  main='com.twitter.hello.heroku.HelloWorldServerMain',
  dependencies=[
    ':hello-world-heroku'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
