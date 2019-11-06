scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-modules",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "finatra/inject/inject-core/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
)
