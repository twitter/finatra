scala_library(
    sources = ["*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-internal-routing",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-thrift/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject/internal",
        "scrooge/scrooge-core/src/main/scala",
    ],
)
