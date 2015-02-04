junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-httpclient',
    'finatra/finatra/finatra-httpclient/src/test/scala:test-deps',
    'finatra/finatra/finatra-utils:tests'
  ],
  sources=globs(
    'com/twitter/finatra/httpclient/*.scala')
)


scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-httpclient-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/finatra/finatra-httpclient',
    'finatra/finatra/finatra-utils:test-deps'
  ],
  sources=globs(
    'com/twitter/finatra/httpclient/test/*.scala'
  ),
)
