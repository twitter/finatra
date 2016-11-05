scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-thrift',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/inject/inject-utils',
    'scrooge/scrooge-core',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-mux/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
