scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/commons-lang',
    'finagle/finagle-core',
    'finagle/finagle-mux',
    'finatra/inject/inject-core',
    'util/util-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-utils/src/main/resources'
  ]
)
