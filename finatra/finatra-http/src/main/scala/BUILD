scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-http',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:commons-fileupload',
    '3rdparty:commons-io',
    '3rdparty:grizzled-slf4j',
    '3rdparty:guava',
    '3rdparty:guice',
    '3rdparty:jackson-core',
    '3rdparty:jackson-databind',
    '3rdparty:javax.inject',
    '3rdparty/jvm/joda-time',
    '3rdparty:mustache-compiler',
    '3rdparty:netty',
    '3rdparty:servlet-api',
    '3rdparty:scala-guice',
    'finagle/finagle-core',
    'finagle/finagle-http',
    'finatra/finatra/finatra-http/src/main/java',
    'finatra/finatra/finatra-jackson',
    'finatra/finatra/finatra-server',
    'finatra/finatra/finatra-utils',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-http/src/main/resources'
  ]
)

java_library(name='java',
  dependencies=[
    'finatra/finatra/finatra-http/src/main/scala'
  ],
  sources=rglobs('*.java')
)

