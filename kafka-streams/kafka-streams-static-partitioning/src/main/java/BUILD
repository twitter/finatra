java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter",
        name = "finatra-streams-static-partitioning-java",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
    ],
    exports = [
    ],
)
