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
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-joda",
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/hibernate/validator:hibernate-validator",
        "3rdparty/jvm/org/json4s:json4s-core",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "finatra/validation/src/main/java/com/twitter/finatra/validation:pants-workaround",
        "util/util-core:scala",
        "util/util-reflect/src/main/scala/com/twitter/util/reflect",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
        "util/util-validator/src/main/scala/com/twitter/util/validation/constraintvalidation",
    ],
    exports = [
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "3rdparty/jvm/joda-time",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
        "util/util-validator/src/main/scala/com/twitter/util/validation/constraintvalidation",
    ],
)
