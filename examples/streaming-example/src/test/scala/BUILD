junit_tests(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "finatra/examples/streaming-example/src/main/scala",
        "finatra/http/src/test/scala:test-deps",
        "finatra/httpclient/src/main/scala",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-server/src/test/scala:test-deps",
        "finatra/jackson/src/main/scala",
    ],
)
