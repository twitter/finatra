scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-thrift',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-mux/src/main/scala:scala',
    'finatra/inject/inject-utils/src/main/scala:scala',
    'scrooge/scrooge-core/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
