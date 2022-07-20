scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-validation-tests",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
    ],
    exports = [
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
)
