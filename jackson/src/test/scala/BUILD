junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'jackson/src/main/scala',
    'jackson/src/main/java',
    'jackson/src/test/scala:test-deps',
    'slf4j',
    'util/util-core'
  ],
  sources=rglobs(
    'com/twitter/tests/*.scala',
  ),
  resources=[
    'jackson/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'jackson-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    'inject/inject-core:test-deps',
    'jackson/src/main/scala',
  ],
  sources=globs(
    'com/twitter/json/*.scala',
    'com/twitter/validation/*.scala',
  ),
  java_sources=[
    'jackson/src/test/java'
  ],
)
