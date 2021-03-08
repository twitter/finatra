FINATRA_KAFKA_CROSS = [
    {
        "kafka": "",
    },
    {
        "kafka": "2.5",
    },
]

cross_junit_tests(
    name = "scala",
    sources = [
        # We can use scala-kafka2.5 directory for Kafka 2.4
        # The below entries are trying to achieve the same effect in Pants and Bazel.
        # This is taken in Pants; entry starting with ".." will be ignored by Bazel
        "../scala-kafka2.5/**/*.scala",
        # This is a bazel filegroup target, however, Pants will see this as a path to match files,
        # which would return nothing.
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/test/scala-kafka2.5:kafka2.5",
    ],
    compiler_option_sets = ["fatal_warnings"],
    cross_build = FINATRA_KAFKA_CROSS,
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-test{-kafka}",
        "3rdparty/jvm/org/scalatestplus:junit",
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala:scala{_kafka_kafka}",
        "finatra/kafka/src/test/scala:test-deps{_kafka_kafka}",
    ],
)
