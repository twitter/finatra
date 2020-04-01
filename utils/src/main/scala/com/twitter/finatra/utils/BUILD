scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-utils",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/sun/activation:javax.activation",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-slf4j/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/resources",  # for META-INF/mime.types
    ],
    exports = [
        "finatra/inject/inject-slf4j/src/main/scala",
    ],
)
