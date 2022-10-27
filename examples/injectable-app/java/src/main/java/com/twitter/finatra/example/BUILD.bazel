java_library(
    sources = ["**/*.java"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finatra/inject/inject-app/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/inject/inject-server/src/main/scala",
        "twitter-server/server/src/main/scala",
        "util/util-app/src/main/scala",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)

jvm_binary(
    name = "bin",
    basename = "injectable-app-java",
    main = "com.twitter.finatra.example.HelloWorldAppMain",
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        ":example",
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "finatra/examples/injectable-app/java/src/main/resources",
    ],
)
