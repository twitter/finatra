scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-thrift",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-utils/src/main/scala",
        "scrooge/scrooge-core/src/main/scala",
        "util/util-core:scala",
    ],
)
