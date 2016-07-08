scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-http',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-core',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/github/spullara/mustache/java',
    '3rdparty/jvm/commons-fileupload',
    '3rdparty/jvm/commons-io',
    '3rdparty/jvm/io/netty',
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/javax/servlet:servlet-api',
    '3rdparty/jvm/joda-time',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/http/src/main/java',
    'finatra/inject/inject-request-scope',
    'finatra/inject/inject-server',
    'finatra/inject/inject-utils',
    'finatra/jackson',
    'finatra/slf4j',
    'finatra/utils',
    'util/util-core',
    'util/util-stats',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/http/src/main/resources'
  ]
)
