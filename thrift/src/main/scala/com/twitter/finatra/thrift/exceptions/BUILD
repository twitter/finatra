scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finatra/inject/inject-core/src/main/scala",
        "util/util-stats/src/main/scala/com/twitter/finagle/stats",
    ],
)
