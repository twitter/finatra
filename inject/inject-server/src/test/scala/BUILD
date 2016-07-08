junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'finatra/inject/inject-server',
    'finatra/inject/inject-server:test-deps',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/server/tests/*.scala',
    'com/twitter/inject/server/tests/*.scala'),
  resources=[
    'finatra/inject/inject-server/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-server-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/inject/inject-app:test-deps',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-modules:test-deps',
    'finatra/inject/inject-server'
  ],
  fatal_warnings=True,
  sources=globs('com/twitter/inject/server/*.scala'),
)
