java_library(
    sources = rglobs("*.java"),
    fatal_warnings = False,
    provides = artifact(
        org = "com.twitter.finatra",
        name = "finatra-http-java",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finagle/finagle-base-http/src/main/scala",
        "finagle/finagle-http/src/main/scala",
    ],
)
