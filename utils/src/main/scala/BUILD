scala_library(
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils",
        repo = artifactory,
    ),
    tags = {
        # users should depend on specific finer-grained targets instead of this monolithic target
        "deprecated",
    },
    dependencies = [
        "finatra/utils/src/main/java/com/twitter/finatra/annotations",
        "finatra/utils/src/main/scala/com/twitter/finatra/filters",
        "finatra/utils/src/main/scala/com/twitter/finatra/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "finatra/utils/src/main/java/com/twitter/finatra/annotations",
        "finatra/utils/src/main/scala/com/twitter/finatra/filters",
        "finatra/utils/src/main/scala/com/twitter/finatra/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
)
