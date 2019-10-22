scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-marshalling-mustache",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/http/src/main/scala/com/twitter/finatra/http/marshalling",
        "util/util-core/src/main/scala/com/twitter/io",
    ],
    exports = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/http/src/main/scala/com/twitter/finatra/http/marshalling",
        "util/util-core/src/main/scala/com/twitter/io",
    ],
)
