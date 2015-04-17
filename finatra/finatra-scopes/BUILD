# finatra/finatra/finatra-scopes has been moved to finatra/inject/inject-request-scope
# This BUILD file redirects current usages to deprecated classes in finatra-utils.
# Clients are expected to follow the deprecation instructions to switch to the new classes in inject-request-scope.
target(name='finatra-scopes',
  dependencies=[
    'finatra/finatra/finatra-utils',
  ]
)

target(name='test-deps',
  dependencies=[
    'finatra/finatra/finatra-utils:test-deps',
  ]
)
