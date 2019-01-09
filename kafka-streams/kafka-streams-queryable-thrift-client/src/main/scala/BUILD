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
    strict_deps = False,
    dependencies = [
        "3rdparty/jvm/com/twitter/bijection:core",
        "finagle/finagle-serversets",
        "finatra/inject/inject-thrift-client",
    ],
    exports = [
        "3rdparty/jvm/com/twitter/bijection:core",
        "finagle/finagle-serversets",
        "finatra/inject/inject-thrift-client",
    ],
)
