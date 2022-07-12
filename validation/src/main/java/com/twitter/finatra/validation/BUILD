java_library(
    sources = [
        "*.java",
        "constraints/*.java",
    ],
    compiler_option_sets = ["fatal_warnings"],
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
    exports = [
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
)

# TODO: Change this to filegroup per https://github.com/pantsbuild/pants/issues/7200
# when we switch entirely to bazel
files(
    name = "pants-workaround",
    sources = ["*.java"],
    tags = ["bazel-compatible"],
)
