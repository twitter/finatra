scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-json-core",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
