scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-serde",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-joda",
        "3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala",
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/json4s:json4s-core",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/exceptions",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
)
