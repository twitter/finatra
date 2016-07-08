scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-thrift',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-mux',
    'finatra/inject/inject-utils',
    'scrooge/scrooge-core',
    'util/util-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
