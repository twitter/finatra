java_library(
    sources = ["**/*.java"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "twitter-server/server/src/main/scala",
        "util/util-core/src/main/java/com/twitter/util",
    ],
)

jvm_binary(
    name = "bin",
    basename = "injectable-twitter-server-java",
    main = "com.twitter.finatra.example.ExampleTwitterServerMain",
    dependencies = [
        ":example",
        "finatra/examples/injectable-twitter-server/java/src/main/resources",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
