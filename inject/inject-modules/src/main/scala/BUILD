scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'inject-modules',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finagle/finagle-core',
    'finatra/inject/inject-core',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
