scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-modules-internal",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject/internal",
    ],
)
