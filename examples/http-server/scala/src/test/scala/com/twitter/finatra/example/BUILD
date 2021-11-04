junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/examples/http-server/scala/src/main/scala/com/twitter/finatra/example",
        "finatra/http-server/src/test/scala/com/twitter/finatra/http",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
    ],
)
