scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-logback',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:grizzled-slf4j',
    '3rdparty:logback-classic',
    '3rdparty:jul-to-slf4j',
    '3rdparty:jcl-over-slf4j',
    '3rdparty:log4j-over-slf4j',
    '3rdparty:slf4j-api',
    'finatra/finatra/finatra-utils',
    'loglens/loglens-logging',
    'util/util-core'
  ],
  sources=rglobs('*.scala')
)
