scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-dtab",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
    ],
    exports = [
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
    ],
)
