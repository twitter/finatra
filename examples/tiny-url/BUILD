target(name='tiny-url',
  dependencies=[
    'finatra/examples/tiny-url/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/tiny-url/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='tiny-url',
  main='com.twitter.tiny.TinyUrlServerMain',
  dependencies=[
    ':tiny-url'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
