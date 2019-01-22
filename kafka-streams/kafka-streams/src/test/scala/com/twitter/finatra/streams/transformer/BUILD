junit_tests(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    strict_deps = False,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "finatra/kafka-streams/kafka-streams/src/main/scala",
        "finatra/kafka-streams/kafka-streams/src/test/resources",
        "finatra/kafka-streams/kafka-streams/src/test/scala/com/twitter:test-deps",
        "finatra/kafka/src/test/scala:test-deps",
    ],
)
