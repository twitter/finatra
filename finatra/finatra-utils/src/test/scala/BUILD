junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-utils',
    'finatra/finatra/finatra-utils:test-deps',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-server:test-deps',
  ],
  sources=rglobs(
    'com/twitter/finatra/tests/*.scala',
  ),
  resources=[
    'finatra/finatra/finatra-utils/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-utils-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/scalatest',
    'finagle/finagle-stats',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-server:test-deps',
  ],
  sources=globs(
    'com/twitter/finatra/guice/*.scala',
    'com/twitter/finatra/test/*.scala'
  )
)
