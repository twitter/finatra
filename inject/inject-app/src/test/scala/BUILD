junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty:logback-classic',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-app',
    'finatra/inject/inject-app:test-deps',
    'finatra/inject/inject-core:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/app/tests/*.scala',
  ),
  resources=[
    'finatra/inject/inject-app/src/test/resources'
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
    'finatra/inject/inject-app',
    'finatra/inject/inject-core',
    'finatra/inject/inject-core:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/app/*.scala',
  ),
  java_sources=[
    'finatra/inject/inject-app/src/test/java'
  ]
)
