java_library(
    sources = rglobs(
        "com/twitter/finatra/kafkastreams/*.java",
    ),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter",
        name = "finatra-streams-java",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
    ],
    exports = [
        "3rdparty/jvm/org/apache/kafka:kafka-clients",
        "3rdparty/jvm/org/apache/kafka:kafka-streams",
    ],
)
