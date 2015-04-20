scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-http',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-core',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/github/spullara/mustache/java:mustache-compiler',
    '3rdparty/jvm/commons-fileupload',
    '3rdparty/jvm/commons-io',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/javax/servlet:servlet-api',
    '3rdparty/jvm/joda-time',
    '3rdparty/jvm/io/netty',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-http/src/main/java',
    'finatra/finatra/finatra-jackson',
    'finatra/inject/inject-server',
    'finatra/finatra/finatra-utils',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-http/src/main/resources'
  ]
)
