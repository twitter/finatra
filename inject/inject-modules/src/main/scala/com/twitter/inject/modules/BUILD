scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules/internal",
        "finatra/inject/inject-stack/src/main/scala/com/twitter/inject",
        "util/util-stats/src/main/scala/com/twitter/finagle/stats",
    ],
)
