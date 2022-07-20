scala_library(
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-test-tests",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "util/util-core/src/main/scala/com/twitter/io",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/org/scalatest",
        "util/util-core/src/main/scala/com/twitter/io",
        "util/util-stats/src/main/scala",
    ],
)
