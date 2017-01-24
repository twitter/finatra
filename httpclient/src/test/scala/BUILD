junit_tests(
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-joda',
    '3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject/extensions:guice-testlib',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/net/codingwell:scala-guice',
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/specs2:mock',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-http/src/main/scala:scala',
    'finatra/httpclient/src/main/scala:scala',
    'finatra/httpclient/src/test/scala:test-deps',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'finatra/jackson/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  sources=globs(
    'com/twitter/finatra/httpclient/*.scala'
  ),
  resources=[
    'finatra/httpclient/src/test/resources',
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-httpclient-tests',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/clapper:grizzled-slf4j',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-http/src/main/scala:scala',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/utils/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/httpclient/test/*.scala'
  ),
)
