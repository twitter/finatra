junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-http/src/test/scala:test-deps',
    'finatra/finatra/finatra-http/src/test/java',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-logback',
    'finatra/inject/inject-server:test-deps',
    'finatra/finatra/finatra-utils:test-deps',
  ],
  sources=rglobs(
    'com/twitter/finatra/CallbackConverterIntegrationTest.scala',
    'com/twitter/finatra/PathPatternTest.scala',
    'com/twitter/finatra/conversions/*.scala',
    'com/twitter/finatra/example/*.scala',
    'com/twitter/finatra/internal/*.scala',
    'com/twitter/finatra/integration/*.scala',
    'com/twitter/finatra/request/*.scala',
    'com/twitter/finatra/response/*.scala',
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
    'finagle/finagle-http',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/inject/inject-server:test-deps',
    'finatra/finatra/finatra-utils:test-deps'
  ],
  sources=globs(
    'com/twitter/finatra/test/*.scala'
  ),
)
