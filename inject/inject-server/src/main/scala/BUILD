scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-server',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-stats',
    'finatra/inject/inject-app',
    'finatra/inject/inject-core',
    'finatra/inject/inject-modules',
    'twitter-server',
    'finagle/finagle-httpx',
    'util/util-app',
    'util/util-core',
    'util/util-stats',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-server/src/main/resources'
  ]
)
