target(name='twitter-clone',
  dependencies=[
    'finatra/examples/twitter-clone/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/examples/twitter-clone/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  basename='finatra-twitter-clone',
  main='finatra.quickstart.TwitterCloneServerMain',
  dependencies=[
    ':twitter-clone'
  ],
  excludes=[
    exclude('org.slf4j', 'slf4j-jdk14'),
    exclude('log4j', 'log4j')
  ]
)
