scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-thrift',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty/jvm/org/yaml:snakeyaml',
    'finagle/finagle-core',
    'finagle/finagle-thriftmux',
    'finatra/thrift/src/main/thrift:thrift-scala',
    'finatra/inject/inject-app',
    'finatra/inject/inject-core',
    'finatra/inject/inject-server',
    'finatra/inject/inject-thrift',
    'finatra/slf4j',
    'finatra/utils',
    'util/util-core'
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala')
)
