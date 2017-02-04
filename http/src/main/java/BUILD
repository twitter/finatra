java_library(
  provides = artifact(
    org = 'com.twitter.finatra',
    name = 'finatra-http-java',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    'finagle/finagle-base-http/src/main/scala:scala',
    'finagle/finagle-http/src/main/scala:scala',
  ],
  fatal_warnings=False,
  sources=rglobs('*.java')
)
