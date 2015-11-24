target(name='streaming-example',
  dependencies=[
    'finatra/examples/streaming-example/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/streaming-example/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='streaming-example',
  main='com.twitter.streaming.StreamingServerMain',
  dependencies=[
    ':streaming-example'
  ],
  deploy_excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
