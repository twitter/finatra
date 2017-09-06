scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-thrift',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core/src/main/scala',
    'finagle/finagle-mux/src/main/scala',
    'finatra/inject/inject-utils/src/main/scala',
    'scrooge/scrooge-core/src/main/scala',
    'util/util-core/src/main/scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  strict_deps=True,
)
