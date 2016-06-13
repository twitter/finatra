junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-app:test-deps',
    'finatra/inject/inject-modules',
    'finatra/inject/inject-modules:test-deps',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/modules/tests/*.scala',
  ),
  resources=[
    'finatra/inject/inject-modules/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-modules-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finatra/inject/inject-core',
    'util/util-stats',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/modules/*.scala',
  )
)
