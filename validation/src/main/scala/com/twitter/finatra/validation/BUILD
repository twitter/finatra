scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
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
        "//:scala-reflect",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/joda-time",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/validation/src/main/java/com/twitter/finatra/validation:pants-workaround",
        "finatra/validation/src/main/resources",
        "util/util-core/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/joda-time",
    ],
)
