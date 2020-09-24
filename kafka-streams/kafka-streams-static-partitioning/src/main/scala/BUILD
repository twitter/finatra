scala_library(
    sources = [
        "../scala-kafka2.2/**/*.scala",
        "com/**/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams-static-partitioning",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/java",
        "finatra/kafka-streams/kafka-streams/src/main/scala",
    ],
    exports = [
        "finatra/kafka-streams/kafka-streams/src/main/scala",
    ],
)
