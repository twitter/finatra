scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.finatra',
    name = 'inject-thrift-client-http-mapper',
    repo = artifactory,
  ),
  dependencies=[
    'finatra/http',
    'finatra/inject/inject-thrift-client',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
