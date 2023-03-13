scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-mysql-util",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/joda-time",
        "finagle/finagle-mysql/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "util/util-stats",
    ],
)
