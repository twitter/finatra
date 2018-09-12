java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter.inject",
        name = "inject-app-java",
        repo = artifactory,
    ),
    scope = "forced",
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
