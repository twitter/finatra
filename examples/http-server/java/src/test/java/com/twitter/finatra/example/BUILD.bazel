junit_tests(
    sources = ["**/*.java"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finagle/finagle-base-http/src/main",
        "finatra/examples/http-server/java/src/main/java/com/twitter/finatra/example",
        "finatra/http-client/src/main/scala",
        "finatra/http-server/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-app/src/main/scala",
        "finatra/inject/inject-server/src/main/scala",
        "twitter-server/server/src/main/scala",
    ],
)
