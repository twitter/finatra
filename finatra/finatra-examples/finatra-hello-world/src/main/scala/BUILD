scala_library(
  name='scala',
  dependencies=[
    '3rdparty/jvm/javax/inject:javax.inject',
    '3rdparty:guice',
    'finatra/finatra/finatra-http',
    'finatra/finatra/finatra-logback',
  ],
  excludes = [	
    exclude(org='org.slf4j', name='slf4j-jdk14')	
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-examples/finatra-hello-world/src/main/resources'
  ]
)
