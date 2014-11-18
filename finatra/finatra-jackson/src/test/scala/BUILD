junit_tests(name='scala',
  dependencies=[
    '3rdparty:junit',
    '3rdparty:logback-classic',
    '3rdparty:mockito-all',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-jackson/src/main/java',
    'finatra/finatra/finatra-jackson/src/test/java',
    'finatra/finatra/finatra-jackson/src/test/scala:test-deps',
    'util/util-core'
  ],
  sources=rglobs(
    'com/twitter/finatra/tests/*.scala',
  ),
  resources=[
    'finatra/finatra/finatra-jackson/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-jackson-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:junit',
    '3rdparty:mockito-all',
    '3rdparty:scalatest',
    '3rdparty:specs2',
    'finatra/finatra/finatra-jackson/src/main/scala',
  ],
  sources=rglobs(
    'com/twitter/finatra/json/*.scala',
    'com/twitter/finatra/conversions/*.scala',
  ),
)
