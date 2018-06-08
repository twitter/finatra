scala_library(
    sources = rglobs("*.scala"),
    fatal_warnings = True,
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-slf4j",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "3rdparty/jvm/com/google/guava",
        "finagle/finagle-core/src/main/scala",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
