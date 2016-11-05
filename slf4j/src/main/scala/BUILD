scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-slf4j',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/inject/inject-core',
    'finatra/inject/inject-slf4j',
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala')
)
