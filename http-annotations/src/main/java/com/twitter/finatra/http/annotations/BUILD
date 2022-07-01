java_library(
    sources = ["*.java"],
    compiler_option_sets = [],
    platform = "java8",
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-java-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "util/util-jackson-annotations/src/main/java/com/twitter/util/jackson/annotation",
    ],
    exports = [
        "util/util-jackson-annotations/src/main/java/com/twitter/util/jackson/annotation",
    ],
)
