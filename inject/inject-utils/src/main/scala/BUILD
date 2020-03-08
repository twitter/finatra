scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-utils",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/github/nscala_time",
        "3rdparty/jvm/joda-time",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/github/nscala_time",
    ],
)
