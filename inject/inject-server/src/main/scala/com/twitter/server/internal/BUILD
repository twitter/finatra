scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-server-internal",
        repo = artifactory,
    ),
    strict_deps = "True",
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
