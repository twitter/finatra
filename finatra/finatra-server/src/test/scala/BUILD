junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-server',
    'finatra/finatra/finatra-server/src/test/scala:test-deps',
    'finatra/finatra/finatra-utils:test-deps'
  ],
  sources=globs('com/twitter/finatra/integration/*.scala'),
  resources=[
    'finatra/finatra/finatra-server/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-server-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-server',
    'finatra/finatra/finatra-utils:test-deps',
  ],
  sources=globs('com/twitter/finatra/test/*.scala'),
)
