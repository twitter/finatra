java_library(
    sources = ["**/*.java"],
    compiler_option_sets = [],
    platform = "java8",
    provides = artifact(
        org = "com.twitter.finatra",
        name = "finatra-utils-java-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
