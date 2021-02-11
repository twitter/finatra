junit_tests(
    sources = [
        # The below entries are trying to achieve the same effect in Pants and Bazel.
        # This is taken in Pants; entry starting with ".." will be ignored by Bazel
        "../scala-kafka2.5/**/*.scala",
        # This is a bazel filegroup target, however, Pants will see this as a path to match files,
        # which would return nothing.
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/test/scala-kafka2.5:kafka-2.5",
    ],
    compiler_option_sets = ["fatal_warnings"],
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test",
        "3rdparty/jvm/org/apache/kafka:kafka-test",
        "3rdparty/jvm/org/scalatestplus:junit",
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala",
        "finatra/kafka/src/test/scala:test-deps",
    ],
)
