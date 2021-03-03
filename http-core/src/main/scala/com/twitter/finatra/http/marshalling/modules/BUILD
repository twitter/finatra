scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-marshalling-modules",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson",
        "finatra/utils/src/main/scala/com/twitter/finatra/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
)
