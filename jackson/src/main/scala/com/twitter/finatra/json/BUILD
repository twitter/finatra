scala_library(
    sources = ["*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-json-core",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/jackson/src/main/scala/com/fasterxml/jackson/databind",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
    ],
    exports = [
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
    ],
)
