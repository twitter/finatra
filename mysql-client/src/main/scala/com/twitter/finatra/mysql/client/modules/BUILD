scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-mysql-client-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finagle/finagle-mysql/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/mysql-client/src/main/scala/com/twitter/finatra/mysql/client",
        "util/util-stats/src/main/scala/com/twitter/finagle/stats",
    ],
)
