junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-utils',
    'finatra/finatra/finatra-utils:test-deps',
    'finatra/finatra/finatra-utils/src/test/java',
  ],
  sources=globs(
    'com/twitter/finatra/annotations/*.scala',
    'com/twitter/finatra/conversions/*.scala',
    'com/twitter/finatra/exceptions/*.scala',
    'com/twitter/finatra/integration/*.scala',
    'com/twitter/finatra/integration/app/*.scala',
    'com/twitter/finatra/logging/*.scala',
    'com/twitter/finatra/utils/*.scala',
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
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-utils',
  ],
  sources=globs(
    'com/twitter/finatra/test/*.scala',
    'com/twitter/finatra/guice/*.scala',
    'org/specs2/matcher/ScalaTestExpectations.scala',
  ),
  java_sources=[
    'finatra/finatra/finatra-utils/src/test/java'
  ]
)
