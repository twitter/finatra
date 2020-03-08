scala_library(
    sources = ["*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-json-utils",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/jackson/src/main/scala/com/fasterxml/jackson/databind",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
    ],
)
