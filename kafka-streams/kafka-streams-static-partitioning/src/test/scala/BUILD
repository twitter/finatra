junit_tests(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    strict_deps = False,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test",
        "3rdparty/jvm/org/apache/kafka:kafka-test",
        "3rdparty/jvm/org/apache/zookeeper:zookeeper-client",
        "finatra/kafka-streams/kafka-streams-static-partitioning/src/main/scala",
        "finatra/kafka-streams/kafka-streams/src/test/scala:test-deps",
        "finatra/kafka/src/test/scala:test-deps",
    ],
)
