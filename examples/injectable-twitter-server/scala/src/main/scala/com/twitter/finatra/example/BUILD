scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
)

jvm_binary(
    name = "bin",
    basename = "injectable-twitter-server-java",
    main = "com.twitter.finatra.example.ExampleTwitterServerMain",
    tags = ["bazel-compatible"],
    dependencies = [
        ":example",
        "finatra/examples/injectable-twitter-server/scala/src/main/resources",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
