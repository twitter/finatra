scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-json-core",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
    exports = [
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
