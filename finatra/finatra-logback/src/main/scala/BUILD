scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-logback',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:grizzled-slf4j',
    '3rdparty:logback-classic',
    '3rdparty/jvm/org/slf4j:jul-to-slf4j',
    '3rdparty/jvm/org/slf4j:jcl-over-slf4j',
    '3rdparty/jvm/org/slf4j:log4j-over-slf4j',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finatra/finatra/finatra-utils',
    'loglens/loglens-logging',
    'util/util-core'
  ],
  sources=rglobs('*.scala')
)
