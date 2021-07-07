java_library(
    sources = ["*.java"],
    compiler_option_sets = [],
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-mustache-java-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/http-annotations/src/main/java/com/twitter/finatra/http/annotations",
    ],
    exports = [
        "finatra/http-annotations/src/main/java/com/twitter/finatra/http/annotations",
    ],
)
