junit_tests(name='scala',
  dependencies=[
    'finatra/finatra/finatra-jackson/src/main/scala',
    'finatra/finatra/finatra-jackson/src/main/java',
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
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    'finatra/finatra/finatra-jackson/src/main/scala',
  ],
  sources=rglobs(
    'com/twitter/finatra/json/*.scala'
  ),
  java_sources=[
    'finatra/finatra/finatra-jackson/src/test/java'
  ],
)
