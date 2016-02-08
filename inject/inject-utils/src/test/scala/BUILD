junit_tests(name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    'finatra/inject/inject-core',
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-utils',
  ],
  fatal_warnings=True,
  sources=globs('com/twitter/inject/tests/conversions/*.scala'),
  resources=[
    'finatra/inject/inject-utils/src/test/resources'
  ]
)

scala_library(name="test-deps",
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-utils-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/inject/inject-core:test-deps',
    'finatra/inject/inject-utils'
  ],
  fatal_warnings=True,
  sources=globs('*.scala'),
)
