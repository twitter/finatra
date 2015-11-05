scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'http-thrift-integration',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/http',
    'finatra/inject/inject-thrift-client',
  ],
  sources=rglobs('*.scala'),
)
