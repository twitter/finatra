junit_tests(
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    '3rdparty/jvm/org/scalatest',
    'finatra/inject/inject-app/src/main/scala:scala',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'util/util-app/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/app/tests/*.scala',
    'com/twitter/inject/app/tests/internal/*.scala'
  ),
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-app-tests',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finatra/inject/inject-app/src/main/scala:scala',
    'finatra/inject/inject-core/src/main/scala:scala',
    'util/util-app/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/app/*.scala',
  )
)
