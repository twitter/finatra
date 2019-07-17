scala_library(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    java_sources = [
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/java",
    ],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams-queryable-thrift-client",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-serversets",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-thrift-client/src/main/scala",
    ],
    exports = [
        "finagle/finagle-serversets",
    ],
)
