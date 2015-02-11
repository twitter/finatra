scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-server',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-stats',
    'finatra/finatra/finatra-utils',
    'twitter-server',
    'util/util-app',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-server/src/main/resources'
  ]
)
