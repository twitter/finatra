junit_tests(name='scala',
  dependencies=[
    '3rdparty:junit',
    '3rdparty:mockito-all',
    '3rdparty:netty',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:tests',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-http/src/test/scala:test-deps',
    'finatra/finatra/finatra-logback',
    'finatra/finatra/finatra-server:tests',
    'finatra/finatra/finatra-utils:tests',
  ],
  sources=rglobs(
    'com/twitter/finatra/CallbackConverterIntegrationTest.scala',
    'com/twitter/finatra/PathPatternTest.scala',
    'com/twitter/finatra/conversions/*.scala',
    'com/twitter/finatra/example/*.scala',
    'com/twitter/finatra/integration/*.scala',
    'com/twitter/finatra/request/*.scala',
    'com/twitter/finatra/routing/*.scala',
    'com/twitter/finatra/server/*.scala',
    'com/twitter/finatra/twitterserver/*.scala',
  ),
  resources=[
    'finatra/finatra/finatra-http/src/test/resources',
    'finatra/finatra/finatra-http/src/test/webapp'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-http-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:junit',
    '3rdparty:mockito-all',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-server:test-deps',
    'finatra/finatra/finatra-utils:test-deps'
  ],
  sources=globs(
    'com/twitter/finatra/test/*.scala'
  ),
)
