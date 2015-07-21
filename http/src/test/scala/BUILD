junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'jackson:test-deps',
    'slf4j',
    'utils:test-deps',
    'http',
    'httpclient',
    'http/src/test/java',
    'http/src/test/scala:test-deps',
    'inject/inject-server:test-deps',
    'util/util-core',
  ],
  sources=rglobs(
    'com/twitter/http/CallbackConverterIntegrationTest.scala',
    'com/twitter/http/PathPatternTest.scala',
    'com/twitter/http/conversions/*.scala',
    'com/twitter/http/example/*.scala',
    'com/twitter/http/integration/*.scala',
    'com/twitter/http/internal/*.scala',
    'com/twitter/http/marshalling/*.scala',
    'com/twitter/http/request/*.scala',
    'com/twitter/http/response/*.scala',
    'com/twitter/http/routing/*.scala',
    'com/twitter/http/server/*.scala',
    'com/twitter/http/twitterserver/*.scala',
  ),
  resources=[
    'http/src/test/resources',
    'http/src/test/webapp',
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
    'jackson:test-deps',
    'utils:test-deps',
    'http',
    'inject/inject-server:test-deps',
  ],
  sources=globs(
    'com/twitter/test/*.scala',
    'com/twitter/http/test/*.scala',
    'com/twitter/http/utils/*.scala'
  ),
)
