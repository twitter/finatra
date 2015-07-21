scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'http',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-core',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/github/spullara/mustache/java:mustache-compiler',
    '3rdparty/jvm/commons-fileupload',
    '3rdparty/jvm/commons-io',
    '3rdparty/jvm/io/netty',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/javax/servlet:servlet-api',
    '3rdparty/jvm/joda-time',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'http/src/main/java',
    'inject/inject-server',
    'jackson',
    'utils',
    'util/util-core',
    'util/util-stats',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'http/src/main/resources'
  ]
)
