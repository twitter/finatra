scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-core-scalatest-expectations",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/specs2:matcher",
    ],
    exports = [
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/specs2:matcher",
    ],
)
