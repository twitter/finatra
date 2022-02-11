scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    java_sources = [
        "finatra/inject/inject-request-scope/src/main/java",
    ],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-request-scope",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-request-scope/src/main/java:pants-workaround",
        "util/util-app/src/main/scala",
        "util/util-core:scala",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject/extensions:guice-assistedinject",
        "3rdparty/jvm/net/codingwell:scala-guice",
    ],
)
