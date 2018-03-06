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
        "3rdparty/jvm/org/slf4j:jcl-over-slf4j",
        "3rdparty/jvm/org/slf4j:jul-to-slf4j",
        "3rdparty/jvm/org/slf4j:log4j-over-slf4j",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
    excludes = [
        exclude(
            org = "org.clapper",
            name = "grizzled-slf4j",
        ),
    ],
    exports = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "util/util-core/src/main/scala",
        "util/util-slf4j-api/src/main/scala",
    ],
)
