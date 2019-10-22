junit_tests(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "finatra/examples/benchmark-server/src/main/scala",
        "finatra/http/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-server/src/main/scala",
    ],
)
