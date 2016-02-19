scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-utils',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/commons-lang',
    'finatra/inject/inject-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/inject/inject-utils/src/main/resources'
  ]
)
