junit_tests(
    sources = ["**/*.java"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/examples/injectable-twitter-server/java/src/main/java/com/twitter/finatra/example",
        "finatra/inject/inject-app/src/main/scala",
        "finatra/inject/inject-app/src/test/scala/com/twitter/inject/app",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
        "twitter-server/server/src/main/scala",
    ],
)
