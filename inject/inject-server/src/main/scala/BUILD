scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-server',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-stats',
    'inject/inject-app',
    'inject/inject-core',
    'inject/inject-modules',
    'twitter-server',
    'util/util-app',
    'util/util-core',
    'util/util-stats',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'inject/inject-server/src/main/resources'
  ]
)
