java_library(
    sources = globs("*.java"),
    compiler_option_sets = {},
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-java-request",
        repo = artifactory,
    ),
    dependencies = [
        "finagle/finagle-base-http/src/main/scala",
    ],
)
