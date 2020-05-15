scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
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
        "util/util-security/src/main/scala/com/twitter/util/security",
    ],
    exports = [
        "finatra/inject/inject-core/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
)
