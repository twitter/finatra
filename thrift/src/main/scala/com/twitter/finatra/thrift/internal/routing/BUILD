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
        "finagle/finagle-thrift/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "scrooge/scrooge-core/src/main/scala",
    ],
)
