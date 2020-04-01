junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-nop",
        "finatra/examples/benchmark-server/src/main/scala",
        "finatra/http/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-server/src/main/scala",
    ],
)
