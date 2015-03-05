junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty:logback-classic',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-core',
    'finatra/inject/inject-core:test-deps',
  ],
  sources=globs(
    'com/twitter/inject/tests/*.scala',
  ),
  resources=[
    'finatra/inject/inject-core/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-core-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-core',
  ],
  sources=globs(
    'com/twitter/inject/*.scala',
    'org/specs2/matcher/ScalaTestExpectations.scala',
  )
)
