junit_tests(
    sources = ["**/*.java"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/inject/inject-app/src/test/scala/com/twitter/inject/app",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/modules",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
