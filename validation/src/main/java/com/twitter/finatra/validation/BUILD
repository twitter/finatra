java_library(
    sources = ["*.java"],
    compiler_option_sets = {},
    strict_deps = True,
    dependencies = [
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
    exports = [
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
    ],
)

# TODO: Remove this and references to it,
# when a fix for https://github.com/pantsbuild/pants/issues/7200 has landed.
files(
    name = "pants-workaround",
    sources = ["*.java"],
)
