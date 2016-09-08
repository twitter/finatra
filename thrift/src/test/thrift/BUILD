create_thrift_libraries(
  base_name='doeverything-thrift',
  sources=globs('*.thrift'),
  generate_languages=["scala", "java"],
  dependency_roots=[
    'finatra/thrift/src/main/thrift:thrift'
  ],
  provides_java_name='doeverything-thrift-java',
  provides_scala_name='doeverything-thrift-scala',
)
