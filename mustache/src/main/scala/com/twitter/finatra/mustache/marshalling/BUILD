scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-mustache-marshalling",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "util/util-core/src/main/scala/com/twitter/io",
    ],
    exports = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-core/src/main/scala",
    ],
)
