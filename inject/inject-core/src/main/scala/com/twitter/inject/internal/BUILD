scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-core-internal",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/joda:joda-convert",
        "util/util-registry/src/main/scala/com/twitter/util/registry",
    ],
)
