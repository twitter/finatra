scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-jackson-caseclass",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-guice",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/exceptions",
        "util/util-jackson-annotations/src/main/java/com/twitter/util/jackson/annotation",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions",
        "util/util-reflect/src/main/scala/com/twitter/util/reflect",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-guice",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
