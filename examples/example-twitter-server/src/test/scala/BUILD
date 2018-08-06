junit_tests(
    sources = rglobs("*.scala"),
    fatal_warnings = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
        "finatra/examples/example-twitter-server/src/main/scala",
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-server/src/test/scala:test-deps",
    ],
)
