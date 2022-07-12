java_library(
    sources = ["**/*.java"],
    compiler_option_sets = [],
    provides = artifact(
        org = "com.twitter.inject",
        name = "inject-request-scope-java",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)

# TODO: Change this to filegroup per https://github.com/pantsbuild/pants/issues/7200
# when we switch entirely to bazel
files(
    name = "pants-workaround",
    sources = ["**/*.java"],
    tags = ["bazel-compatible"],
)
