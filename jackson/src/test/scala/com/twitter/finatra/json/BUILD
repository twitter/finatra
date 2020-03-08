scala_library(
    sources = ["*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-tests",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/org/scalatest",
        "finatra/jackson/src/main/scala/com/twitter/finatra/json/utils",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/org/scalatest",
        "finatra/jackson/src/main/scala/com/twitter/finatra/json/utils",
    ],
)
