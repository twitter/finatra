junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-core',
    'finatra/inject/inject-core:test-deps',
  ],
  fatal_warnings=True,
  sources=rglobs(
    'com/twitter/inject/tests/*.scala',
  )
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-core-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2',
    'finatra/inject/inject-core',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/*.scala',
    'org/specs2/matcher/ScalaTestExpectations.scala',
  ),
  java_sources=[
    'finatra/inject/inject-core/src/test/java'
  ]
)
