scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-caseclass-exceptions",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "finatra/json-annotations/src/main/java/com/twitter/finatra/json/annotations",
        "finatra/validation/src/main/scala/com/twitter/finatra/validation",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
    ],
)
