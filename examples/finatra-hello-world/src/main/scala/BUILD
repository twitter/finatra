scala_library(
  name='scala',
  dependencies=[
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finatra/logback',
    'finatra/http',
  ],
  excludes = [
    exclude(org='org.slf4j', name='slf4j-jdk14')
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/examples/finatra-hello-world/src/main/resources'
  ]
)
