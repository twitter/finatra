scala_library(
  name='scala',
  dependencies=[
    '3rdparty/jvm/ch/qos/logback:logback-classic',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/javax/inject:javax.inject',
    'finatra/http',
    'finatra/httpclient',
    'finatra/jackson',
    'finatra/slf4j',
  ],
  excludes = [
    exclude(org='org.slf4j', name='slf4j-jdk14')
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  resources=[
    'finatra/examples/twitter-clone/src/main/resources'
  ]
)
