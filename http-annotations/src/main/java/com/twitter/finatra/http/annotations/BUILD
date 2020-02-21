java_library(
    sources = ["*.java"],
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-java-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
    exports = [
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
)
