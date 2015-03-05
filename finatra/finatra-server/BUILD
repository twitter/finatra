# finatra/finatra/finatra-server has been moved to finatra/inject/inject-server
# This BUILD file temporarily redirects current users to the new location
target(name='finatra-server',
  dependencies=[
    'finatra/inject/inject-server'
  ]
)

target(name='test-deps',
  dependencies=[
    'finatra/finatra/finatra-http:test-deps',
    'finatra/inject/inject-server:test-deps'
  ]
)
