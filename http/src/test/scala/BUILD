junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'finatra/jackson:test-deps',
    'finatra/slf4j',
    'finatra/utils:test-deps',
    'finatra/http',
    'finatra/httpclient',
    'finatra/httpclient:test-deps',
    'finatra/http/src/test/java:test-deps',
    'finatra/http/src/test/scala:test-deps',
    'finatra/inject/inject-server:test-deps',
    'util/util-core',
  ],
  sources=rglobs(
    'com/twitter/finatra/http/conversions/*.scala',
    'com/twitter/finatra/http/filters/*.scala',
    'com/twitter/finatra/http/integration/*.scala',
    'com/twitter/finatra/http/internal/*.scala',
    'com/twitter/finatra/http/marshalling/*.scala',
    'com/twitter/finatra/http/request/*.scala',
    'com/twitter/finatra/http/response/*.scala',
    'com/twitter/finatra/http/routing/*.scala',
    'com/twitter/finatra/http/server/*.scala',
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
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/http/test/*.scala'
  ),
)
