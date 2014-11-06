junit_tests(name='scala',
  dependencies=[
    '3rdparty:junit',
    '3rdparty:netty',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:tests',
    'finatra/finatra/finatra-server',
    'finatra/finatra/finatra-utils/src/test/java',
    'finatra/finatra/finatra-utils:tests'
  ],
  sources=rglobs('*.scala'),
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
    '3rdparty:junit',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-server',
    'finatra/finatra/finatra-utils/src/test/java',
    'finatra/finatra/finatra-utils:test-deps'
  ],
  sources=globs('com/twitter/finatra/test/*.scala'),
)
