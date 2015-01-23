junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-utils/src/main/scala',
    'finatra/finatra/finatra-utils/src/test/scala:test-deps',
    'finatra/finatra/finatra-utils/src/test/java',
  ],
  sources=rglobs(
    'com/twitter/finatra/conversions/*.scala',
    'com/twitter/finatra/exceptions/*.scala',
    'com/twitter/finatra/integration/*.scala',
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
