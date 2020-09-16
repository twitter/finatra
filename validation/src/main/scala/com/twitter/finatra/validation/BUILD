scala_library(
    sources = [
        "**/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    java_sources = [
        "finatra/validation/src/main/java/com/twitter/finatra/validation",
    ],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-validation-core",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/github/ben-manes/caffeine",
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/json4s:json4s-core",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "finatra/validation/src/main/java/com/twitter/finatra/validation:pants-workaround",
        "finatra/validation/src/main/resources",
        "util/util-core:scala",
    ],
    exports = [
        "3rdparty/jvm/joda-time",
    ],
)
