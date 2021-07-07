scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-utils-utils",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/sun/activation:javax.activation",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/resources",  # for META-INF/mime.types
        "util/util-reflect/src/main/scala/com/twitter/util/reflect",
    ],
    exports = [
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
