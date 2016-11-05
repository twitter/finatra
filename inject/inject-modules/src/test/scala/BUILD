junit_tests(
  dependencies=[
    '3rdparty/jvm/junit',
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/org/scalatest',
    'finatra/inject/inject-modules',
    'finatra/inject/inject-modules:test-deps',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'util/util-stats/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/modules/tests/*.scala',
  ),
  resources=[
    'finatra/inject/inject-modules/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-modules-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finatra/inject/inject-core',
    'util/util-stats',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/inject/modules/*.scala',
  )
)
