scala_library(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-server",
        repo = artifactory,
    ),
    dependencies = [
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-server/src/main/scala/com/twitter/server/internal",
    ],
    exports = [
        "finatra/inject/inject-server/src/main/scala/com/twitter/inject/server",
        "finatra/inject/inject-server/src/main/scala/com/twitter/server/internal",
    ],
)
