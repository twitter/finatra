scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-json-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/modules",
        "finatra/jackson/src/main/scala/com/twitter/finatra/json",
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
    exports = [
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/modules",
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
)
