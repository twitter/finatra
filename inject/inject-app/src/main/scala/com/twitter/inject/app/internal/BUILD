scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-app-internal",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/joda-time",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/console",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "util/util-core:util-core-util",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
    exports = [
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/console",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
    ],
)
