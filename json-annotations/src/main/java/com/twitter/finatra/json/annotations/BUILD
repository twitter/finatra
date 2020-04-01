java_library(
    sources = ["*.java"],
    compiler_option_sets = ["fatal_warnings"],
    provides = artifact(
        org = "com.twitter",
        name = "finatra-json-annotations",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
