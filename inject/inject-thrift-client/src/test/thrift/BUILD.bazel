create_thrift_libraries(
    base_name = "greeter-thrift",
    sources = ["*.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    generate_languages = [
        "java",
        "scala",
    ],
    provides_java_name = "greeter-thrift-java",
    provides_scala_name = "greeter-thrift-scala",
)
