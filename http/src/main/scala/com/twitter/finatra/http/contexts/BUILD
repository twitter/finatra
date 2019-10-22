scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-contexts",
        repo = artifactory,
    ),
    dependencies = [
        "finagle/finagle-base-http/src/main/scala",
    ],
)
