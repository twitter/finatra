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
    'http/src/main/scala'
  ]
)

target(name='tests',
  dependencies=[
    'http/src/test/scala'
  ]
)

target(name='test-deps',
  dependencies=[
    'http/src/test/scala:test-deps'
  ]
)
