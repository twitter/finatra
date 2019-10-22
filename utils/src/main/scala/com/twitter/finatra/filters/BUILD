scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-filters",
        repo = artifactory,
    ),
    dependencies = [
        "finagle/finagle-core/src/main/scala",
    ],
    exports = [
        "finagle/finagle-core/src/main/scala",
    ],
)
