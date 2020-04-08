scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-response",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finagle/finagle-core/src/main",
        "finagle/finagle-thrift/src/main/scala",
        "twitter-server/server/src/main/scala",
    ],
)
