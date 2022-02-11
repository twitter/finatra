scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-mdc",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main",
        "util/util-core/src/main/scala/com/twitter/concurrent",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
