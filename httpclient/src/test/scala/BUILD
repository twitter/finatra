junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'httpclient',
    'httpclient/src/test/scala:test-deps',
    'utils:tests',
    'http',
    'inject/inject-app:test-deps',
  ],
  sources=globs(
    'com/twitter/httpclient/*.scala')
)


scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'httpclient-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'httpclient',
    'utils:test-deps'
  ],
  sources=globs(
    'com/twitter/httpclient/test/*.scala'
  ),
)
