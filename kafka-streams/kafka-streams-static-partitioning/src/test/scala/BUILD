junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test",
        "3rdparty/jvm/org/apache/kafka:kafka-test",
        "finatra/kafka-streams/kafka-streams-queryable-thrift-client/src/main/scala",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala",
        "finatra/kafka/src/test/scala:test-deps",
    ],
)
