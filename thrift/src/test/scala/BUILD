junit_tests(name='scala',
  dependencies=[
    'finatra/slf4j',
    'finatra/thrift/src/test/thrift:echo-thrift-scala',
    'finatra/thrift:test-deps',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/thrift/tests/*.scala',
    'com/twitter/finatra/thrift/tests/doeverything/*.scala',
    'com/twitter/finatra/thrift/tests/doeverything/controllers/*.scala',
    'com/twitter/finatra/thrift/tests/doeverything/filters/*.scala',
  ),
  resources=[
    'finatra/thrift/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-thrift-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/thrift',
    'finatra/inject/inject-app:test-deps',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-server:test-deps',
  ],
  fatal_warnings=True,
  sources=globs('com/twitter/finatra/thrift/*.scala')
)
