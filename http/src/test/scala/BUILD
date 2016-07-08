junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'finatra/jackson:test-deps',
    'finatra/slf4j',
    'finatra/utils:test-deps',
    'finatra/http',
    'finatra/httpclient',
    'finatra/httpclient:test-deps',
    'finatra/http/src/test/java',
    'finatra/http/src/test/scala:test-deps',
    'finatra/inject/inject-server:test-deps',
    'util/util-core',
  ],
  sources=rglobs(
    'com/twitter/finatra/http/tests/*.scala',
  ),
  resources=[
    'finatra/http/src/test/resources',
    'finatra/http/src/test/webapp',
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-http-test-jar',
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
    'com/twitter/finatra/http/*.scala'
  ),
)
