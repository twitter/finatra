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
    '3rdparty/jvm/org/scalatest',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    '3rdparty/jvm/org/slf4j:slf4j-simple',
    '3rdparty/jvm/org/specs2:mock',
    'finagle/finagle-core/src/main/scala',
    'finagle/finagle-http/src/main/scala',
    'finatra/httpclient/src/main/scala',
    'finatra/httpclient/src/test/resources',
    'finatra/httpclient/src/test/scala:test-deps',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'finatra/jackson/src/main/scala',
    'twitter-server/src/main/scala',
    'util/util-core/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  sources=globs(
    'com/twitter/finatra/httpclient/*.scala'
  ),
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-httpclient-tests',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-core/src/main/scala',
    'finagle/finagle-http/src/main/scala',
    'finatra/inject/inject-app/src/test/scala:test-deps',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-core/src/test/scala:test-deps',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'util/util-core/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  fatal_warnings=True,
  sources=globs(
    'com/twitter/finatra/httpclient/test/*.scala'
  ),
)
