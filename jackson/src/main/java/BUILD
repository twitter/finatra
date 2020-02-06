java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter",
        name = "finatra-jackson-java",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
    ],
)
