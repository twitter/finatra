scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-fileupload",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/commons-fileupload",
        "finagle/finagle-base-http/src/main",
    ],
    exports = [
        "3rdparty/jvm/commons-fileupload",
        "finagle/finagle-base-http/src/main",
    ],
)
