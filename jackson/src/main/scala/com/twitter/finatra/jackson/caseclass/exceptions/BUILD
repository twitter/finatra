scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-caseclass-exceptions",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-guice",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "util/util-jackson-annotations/src/main/java/com/twitter/util/jackson/annotation",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions",
    ],
)
