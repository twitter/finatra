scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-slf4j',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject/extensions:guice-assistedinject',
    '3rdparty/jvm/com/google/inject/extensions:guice-multibindings',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/net/codingwell:scala-guice',
    'finatra/inject/inject-core/src/main/scala:scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  excludes=[
    exclude(org='org.clapper', name='grizzled-slf4j')
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala')
)
