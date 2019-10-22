scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-utils",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/sun/activation:javax.activation",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-app/src/main/java",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/resources",
    ],
    exports = [
        "finatra/inject/inject-slf4j/src/main/scala",
    ],
)
