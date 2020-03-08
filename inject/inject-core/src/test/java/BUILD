java_library(
    sources = ["**/*.java"],
    compiler_option_sets = {},
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)

# TODO: Remove this and references to it,
# when a fix for https://github.com/pantsbuild/pants/issues/7200 has landed.
files(
    name = "pants-workaround",
    sources = ["**/*.java"],
)
