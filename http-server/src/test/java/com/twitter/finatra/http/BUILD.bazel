java_library(
    sources = ["*.java"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finatra/http-annotations/src/main/java/com/twitter/finatra/http/annotations",
    ],
)
