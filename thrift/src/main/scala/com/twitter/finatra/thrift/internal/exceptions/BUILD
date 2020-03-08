scala_library(
    sources = ["*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-internal-exceptions",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "util/util-core:util-core-util",
    ],
)
