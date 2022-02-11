scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-slf4j",
        repo = artifactory,
    ),
    tags = [
        "bazel-compatible",
        # users should prefer to depend on util/util-slf4j-api/src/main/scala/com/twitter/util/logging
        "deprecated",
    ],
    dependencies = [
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "util/util-core:scala",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
