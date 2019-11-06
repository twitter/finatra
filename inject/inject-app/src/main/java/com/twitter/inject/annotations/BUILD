java_library(
    sources = globs("*.java"),
    compiler_option_sets = {"fatal_warnings"},
    provides = artifact(
        org = "com.twitter.inject",
        name = "inject-app-java",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
    ],
)
