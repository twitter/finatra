junit_tests(
    sources = globs("*.scala"),
    fatal_warnings = True,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-core",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-logback/src/main/scala",
        "finatra/inject/inject-logback/src/test/scala/ch/qos/logback/core",
        "finatra/inject/inject-logback/src/test/scala/com/twitter/finatra/logback/appender",
        "util/util-stats/src/main/scala",
    ],
)
