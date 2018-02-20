create_thrift_libraries(
    base_name = "thrift",
    sources = rglobs("*.thrift"),
    dependency_roots = [
        "finatra/thrift/src/main/thrift:thrift",
    ],
    generate_languages = [
        "go",
        "java",
        "python",
        "ruby",
        "scala",
    ],
    provides_scala_name = "calculator-thrift-scala",
)
