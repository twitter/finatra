scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-ports",
        repo = artifactory,
    ),
    strict_deps = "True",
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "twitter-server/server/src/main/scala",
        "util/util-app/src/main/scala",
    ],
    exports = [
        "finagle/finagle-core/src/main",
        "twitter-server/server/src/main/scala",
    ],
)
