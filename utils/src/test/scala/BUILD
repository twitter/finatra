junit_tests(
  dependencies=[
    'finatra/utils:test-deps',
    'finatra/inject/inject-core:test-deps',
    '3rdparty/jvm/com/github/nscala_time:nscala_time',
    '3rdparty/jvm/commons-io:commons-io',
    '3rdparty/jvm/joda-time:joda-time',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    '3rdparty/jvm/org/scalatest:scalatest',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-http/src/main/scala:scala',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/utils/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
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
    '3rdparty/jvm/org/scalatest',
    'finagle/finagle-stats',
    'finatra/utils',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finatra/inject/inject-core/src/main/scala:scala',
    'util/util-stats/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/modules/*.scala',
    'com/twitter/finatra/test/*.scala'
  )
)
