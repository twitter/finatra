scala_library(
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-app",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
    ],
    exports = [
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
    ],
)
