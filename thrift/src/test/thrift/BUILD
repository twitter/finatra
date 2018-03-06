create_thrift_libraries(
    base_name = "doeverything-thrift",
    sources = globs("*.thrift"),
    dependency_roots = [
        "finatra/thrift/src/main/thrift:thrift",
    ],
    generate_languages = [
        "java",
        "scala",
    ],
    provides_java_name = "doeverything-thrift-java",
    provides_scala_name = "doeverything-thrift-scala",
)
