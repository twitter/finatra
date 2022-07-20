scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)

jvm_binary(
    name = "bin",
    basename = "injectable-twitter-server-java",
    main = "com.twitter.finatra.example.ExampleTwitterServerMain",
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        ":example",
        "finatra/examples/injectable-twitter-server/scala/src/main/resources",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
