java_library(
    sources = ["*.java"],
    compiler_option_sets = [],
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-java-request",
        repo = artifactory,
    ),
    strict_deps = "True",
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-base-http/src/main",
    ],
)
