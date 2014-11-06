junit_tests(name='scala',
  dependencies=[
    '3rdparty:junit',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-utils',
    'finatra/finatra/finatra-utils/src/test/java'
  ],
  sources=rglobs('*.scala'),
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
    '3rdparty:junit',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson:test-deps',
    'finatra/finatra/finatra-utils',
    'finatra/finatra/finatra-utils/src/test/java'
  ],
  sources = globs(
    'com/twitter/finatra/test/*.scala',
    'com/twitter/finatra/guice/*.scala',
  ),
  java_sources=[
    'finatra/finatra/finatra-utils/src/test/java'
  ]
)
