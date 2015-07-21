junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'inject/inject-app',
    'inject/inject-app:test-deps',
    'inject/inject-core:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/app/tests/*.scala',
  ),
  resources=[
    'inject/inject-app/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-app-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'inject/inject-app',
    'inject/inject-core',
    'inject/inject-core:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/app/*.scala',
  ),
  java_sources=[
    'inject/inject-app/src/test/java'
  ]
)
