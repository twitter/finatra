java_library(
    sources = rglobs("*.java"),
    compiler_option_sets = {},
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
