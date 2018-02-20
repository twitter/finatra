java_library(
    sources = rglobs("*.java"),
    fatal_warnings = False,
    provides = artifact(
        org = "com.twitter.inject",
        name = "inject-thrift-client-java",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
