java_library(
    sources = rglobs("*.java"),
    fatal_warnings = False,
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
