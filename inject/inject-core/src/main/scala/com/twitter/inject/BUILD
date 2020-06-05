scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-core",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "//:scala-reflect",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/joda:joda-convert",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-app/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-app/src/main/scala",
    ],
)
