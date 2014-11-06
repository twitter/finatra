java_library(name='java',
  provides = artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-jackson-java',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:guice',
    'finatra/finatra/finatra-jackson/src/main/scala'
  ],
  sources=rglobs('*.java')
)
