scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-server',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-stats',
    'finagle/finagle-http',
    'finatra/inject/inject-app',
    'finatra/inject/inject-core',
    'finatra/inject/inject-modules',
    'finatra/inject/inject-utils',
    'twitter-server',
    'util/util-app',
    'util/util-core',
    'util/util-stats',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-server/src/main/resources'
  ]
)
