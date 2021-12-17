scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-mysql-tests",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/github/ben-manes/caffeine",
        "finagle/finagle-core/src/main",
        "finagle/finagle-mysql/src/main/scala",
        "finagle/finagle-mysql/src/test/scala/com/twitter/finagle/mysql/harness",
        "finagle/finagle-mysql/src/test/scala/com/twitter/finagle/mysql/harness/config",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
        "finatra/mysql-client/src/main/scala/com/twitter/finatra/mysql/client",
        "util/util-jvm/src/main/scala",
        "util/util-mock/src/main/scala/com/twitter/util/mock",
        "util/util-stats/src/main/scala/com/twitter/finagle/stats",
    ],
)
