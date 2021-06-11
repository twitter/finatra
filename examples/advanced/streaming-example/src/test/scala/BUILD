junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/examples/advanced/streaming-example/src/main/scala",
        "finatra/http-client/src/main/scala",
        "finatra/http-server/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
    ],
)
