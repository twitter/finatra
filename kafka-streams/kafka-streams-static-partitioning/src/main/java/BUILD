java_library(
    sources = ["**/*.java"],
    compiler_option_sets = [],
    provides = artifact(
        org = "com.twitter",
        name = "finatra-streams-static-partitioning-java",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
    ],
    exports = [
    ],
)
