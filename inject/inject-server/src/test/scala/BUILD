junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'inject/inject-server',
    'inject/inject-server:test-deps'
  ],
  sources=globs('com/twitter/inject/server/tests/*.scala'),
  resources=[
    'inject/inject-server/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-server-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'inject/inject-app:test-deps',
    'inject/inject-core:test-deps',
    'inject/inject-modules:test-deps',
    'inject/inject-server'
  ],
  sources=globs('com/twitter/inject/server/*.scala'),
)
