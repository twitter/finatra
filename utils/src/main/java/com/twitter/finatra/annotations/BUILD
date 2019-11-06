java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter.finatra",
        name = "finatra-utils-java-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
