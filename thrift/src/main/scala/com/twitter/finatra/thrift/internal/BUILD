scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-internal",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-core/src/main",
        "scrooge/scrooge-core/src/main/scala",
    ],
)
