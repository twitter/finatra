junit_tests(name='scala',
  dependencies=[
    'finatra/utils:test-deps',
    'finatra/inject/inject-core:test-deps',
  ],
  sources=rglobs(
    'com/twitter/finatra/tests/*.scala',
  ),
  resources=[
    'finatra/utils/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-utils-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/scalatest',
    'finagle/finagle-stats',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-server:test-deps',
    'finatra/utils',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/modules/*.scala',
    'com/twitter/finatra/test/*.scala'
  )
)
