scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-exceptions-json-internal",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/exceptions",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions",
    ],
    exports = [
        "3rdparty/jvm/jakarta/validation:jakarta.validation-api",
        "finatra/jackson/src/main/scala/com/twitter/finatra/jackson/caseclass/exceptions",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson/caseclass/exceptions",
    ],
)
