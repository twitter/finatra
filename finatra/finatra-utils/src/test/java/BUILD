junit_tests(name='tests',
  dependencies=[
    '3rdparty:junit',
    '3rdparty:mockito-all',
    'finatra/finatra/finatra-utils'
  ],
  sources=rglobs('*.java'),
  resources=[
    'finatra/finatra/finatra-utils/src/test/resources'
  ]
)

java_library(name='java',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-utils-java-test-jar',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:guava',
    '3rdparty:guice'
  ],
  sources=rglobs('*.java')
)
