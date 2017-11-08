scala_library(
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'finagle/finagle-http/src/main/scala',
    'finatra/examples/hello-world/src/main/resources',
    'finatra/http/src/main/scala',
    'finatra/inject/inject-app/src/main/scala',
    'finatra/inject/inject-core/src/main/scala',
    'finatra/inject/inject-server/src/main/scala',
    'finatra/inject/inject-slf4j/src/main/scala:scala',
    'twitter-server/server/src/main/scala',
    'util/util-app/src/main/scala',
    'util/util-core/src/main/scala',
    'util/util-lint/src/main/scala:scala',
    'util/util-logging/src/main/scala',
    'util/util-slf4j-api/src/main/scala',
  ],
  excludes = [
    exclude(org='org.slf4j', name='slf4j-jdk14')
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
