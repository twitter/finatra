junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-core",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/http/src/main/scala",
        "finatra/http/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-logback/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
)
