scala_library(
  name='scala',
  dependencies=[
    '3rdparty:guice',
    '3rdparty:javax.inject',
    'finatra/finatra/finatra-logback',
    'finatra/finatra/finatra-http',
  ],
  excludes = [	
    exclude(org='org.slf4j', name='slf4j-jdk14')	
  ],
  sources=rglobs('*.scala'),
  resources=[
    'finatra/finatra/finatra-examples/finatra-hello-world/src/main/resources'
  ]
)
