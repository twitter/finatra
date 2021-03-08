FINATRA_KAFKA_CROSS = [
    {"kafka": ""},
    {"kafka": "2.5"},
]

cross_scala_library(
    name = "scala",
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    cross_build = FINATRA_KAFKA_CROSS,
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-streams-queryable-thrift{-kafka}",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala:scala{_kafka_kafka}",
    ],
)
