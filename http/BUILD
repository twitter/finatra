maven_layout()

page(name="readme",
     source="README.md",
     provides=[
       wiki_artifact(
         wiki=confluence,
         space='PLATENG',
         title='Finatra'
     )])

target(name='http',
  dependencies=[
    'finatra/http/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'finatra/http/src/test/scala'
  ]
)

target(name='test-deps',
  dependencies=[
    'finatra/http/src/test/scala:test-deps'
  ]
)
