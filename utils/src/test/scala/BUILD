junit_tests(
  dependencies=[
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    '3rdparty/jvm/commons-io:commons-io',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/scalatest:scalatest',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    '3rdparty/jvm/org/slf4j:slf4j-simple',
    'finagle/finagle-core/src/main/scala',
    'finagle/finagle-http/src/main/scala',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/scala:scala',
    'finatra/utils/src/main/scala',
    'finatra/utils/src/test/scala:test-deps',
    'util/util-core/src/main/scala',
    'util/util-logging/src/main/scala:scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  sources=rglobs(
    'com/twitter/finatra/tests/*.scala',
  ),
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-utils-tests',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-stats/src/main/scala',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/utils/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
    'util/util-stats/src/main/scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/modules/*.scala',
    'com/twitter/finatra/test/*.scala'
  ),
  exports=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/net/codingwell:scala-guice',
  ]
)
