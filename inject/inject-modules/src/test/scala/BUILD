junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'inject/inject-app:test-deps',
    'inject/inject-modules',
    'inject/inject-modules:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/modules/tests/*.scala',
  ),
  resources=[
    'inject/inject-modules/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-modules-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'inject/inject-core',
    'util/util-stats',
  ],
  sources=globs(
    'com/twitter/inject/modules/*.scala',
  )
)
