junit_tests(name='scala',
  dependencies=[
    'utils',
    'utils:test-deps',
    'inject/inject-core:test-deps',
    'inject/inject-server:test-deps',
  ],
  sources=rglobs(
    'com/twitter/tests/*.scala',
  ),
  resources=[
    'utils/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'utils-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/scalatest',
    'finagle/finagle-stats',
    'inject/inject-core:test-deps',
    'inject/inject-server:test-deps',
  ],
  sources=globs(
    'com/twitter/guice/*.scala',
    'com/twitter/test/*.scala'
  )
)
