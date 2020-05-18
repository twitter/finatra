scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-mdc",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-core/src/main/scala/com/twitter/concurrent",
    ],
    exports = [
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
    ],
)
