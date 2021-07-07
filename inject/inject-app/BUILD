target(
    tags = [
        "bazel-compatible",
        # users should use finer-grain targets directly
        "deprecated",
    ],
    dependencies = [
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
    ],
)

target(
    name = "test-deps",
    tags = [
        "bazel-compatible",
        # users should use finatra/inject/inject-app/src/test/scala/com/twitter/inject/app directly
        "deprecated",
    ],
    dependencies = [
        "finatra/inject/inject-app/src/test/scala/com/twitter/inject/app",
    ],
)
