java_library(
    sources = ["*.java"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = artifact(
        org = "com.twitter",
        name = "finatra-jackson-json-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
    ],
    exports = [
        "3rdparty/jvm/javax/inject:javax.inject",
    ],
)
