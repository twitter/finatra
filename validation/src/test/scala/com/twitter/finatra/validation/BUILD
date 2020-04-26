scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-validation-tests",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
    exports = [
        "finatra/inject/inject-core/src/test/scala:test-deps",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
)
