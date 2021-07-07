scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "util/util-app/src/main/scala",
    ],
)

jvm_binary(
    name = "bin",
    basename = "injectable-app-scala",
    main = "com.twitter.finatra.example.SampleAppMain",
    tags = ["bazel-compatible"],
    dependencies = [
        ":example",
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "finatra/examples/injectable-app/scala/src/main/resources",
    ],
)
