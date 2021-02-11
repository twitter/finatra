scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-mustache-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/http-mustache/src/main/java/com/twitter/finatra/http/annotations",
        "finatra/http-mustache/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/modules",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/marshalling",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/http-mustache/src/main/java/com/twitter/finatra/http/annotations",
        "finatra/mustache/src/main/scala/com/twitter/finatra/mustache/modules",
    ],
)
