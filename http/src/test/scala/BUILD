junit_tests(name='scala',
  dependencies=[
    'finatra/jackson:test-deps',
    'finatra/logback',
    'finatra/utils:test-deps',
    'finatra/http',
    'finatra/httpclient',
    'finatra/http/src/test/java',
    'finatra/http/src/test/scala:test-deps',
    'finatra/inject/inject-server:test-deps',
  ],
  sources=rglobs(
    'com/twitter/finatra/http/CallbackConverterIntegrationTest.scala',
    'com/twitter/finatra/http/PathPatternTest.scala',
    'com/twitter/finatra/http/conversions/*.scala',
    'com/twitter/finatra/http/example/*.scala',
    'com/twitter/finatra/http/integration/*.scala',
    'com/twitter/finatra/http/internal/*.scala',
    'com/twitter/finatra/http/marshalling/*.scala',
    'com/twitter/finatra/http/request/*.scala',
    'com/twitter/finatra/http/response/*.scala',
    'com/twitter/finatra/http/routing/*.scala',
    'com/twitter/finatra/http/server/*.scala',
    'com/twitter/finatra/http/twitterserver/*.scala',
  ),
  resources=[
    'finatra/http/src/test/resources',
    'finatra/http/src/test/webapp',
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'http-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-http',
    'finatra/jackson:test-deps',
    'finatra/utils:test-deps',
    'finatra/http',
    'finatra/inject/inject-server:test-deps',
  ],
  sources=globs(
    'com/twitter/finatra/test/*.scala',
    'com/twitter/finatra/http/test/*.scala',
    'com/twitter/finatra/http/utils/*.scala'
  ),
)
