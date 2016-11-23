junit_tests(
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/scalatest',
    'finatra/inject/inject-core',
    'finatra/inject/inject-core:test-deps',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'util/util-app/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
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
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/commons-io:commons-io',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'util/util-core/src/main/scala:scala',
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
