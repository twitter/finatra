scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-thrift",
        repo = artifactory,
    ),
    dependencies = [
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-mux/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "scrooge/scrooge-core/src/main/scala",
        "util/util-core:scala",
    ],
)
