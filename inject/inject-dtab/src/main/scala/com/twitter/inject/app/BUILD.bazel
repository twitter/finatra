scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-dtab",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
    ],
    exports = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
    ],
)
