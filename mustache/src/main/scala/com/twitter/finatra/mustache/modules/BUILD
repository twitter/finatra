scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-mustache-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/marshalling",
        "finatra/utils/src/main/scala/com/twitter/finatra/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/javax/inject:javax.inject",
    ],
)
