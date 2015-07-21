scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter.inject',
    name = 'inject-modules',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finagle/finagle-core',
    'inject/inject-core',
  ],
  sources=rglobs('*.scala'),
)
