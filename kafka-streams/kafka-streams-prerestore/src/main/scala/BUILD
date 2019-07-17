scala_library(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams-prerestore",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala",
    ],
    exports = [
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala",
    ],
)
