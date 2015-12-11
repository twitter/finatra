junit_tests(name='scala',
  dependencies=[
    'finatra/slf4j',
    'finatra/thrift/src/test/thrift:echo-thrift-scala',
    'finatra/thrift:test-deps',
  ],
  sources=globs('com/twitter/finatra/thrift/tests/*.scala'),
  resources=[
    'finatra/thrift/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'thrift-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/thrift',
    'finatra/inject/inject-app:test-deps',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-server:test-deps',
  ],
  sources=globs('com/twitter/finatra/thrift/*.scala')
)
