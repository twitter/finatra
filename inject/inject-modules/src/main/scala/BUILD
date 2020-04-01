scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-modules",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject/internal",
        "finatra/inject/inject-slf4j/src/main/scala",
        "util/util-app/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
        "util/util-slf4j-jul-bridge/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/net/codingwell:scala-guice",
    ],
)
