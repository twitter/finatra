scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-slf4j",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala",
    ],
    exports = [
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
