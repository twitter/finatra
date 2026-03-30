# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Finatra is a lightweight Scala framework for building HTTP and Thrift servers on top of TwitterServer and Finagle. It provides dependency injection via Google Guice, JSON support via Jackson, and powerful testing utilities.

## Build System

This project uses sbt (Scala Build Tool). **Always use the `./sbt` wrapper script**, not the system sbt.

### Requirements

- **Java**: Java 21 (required)
- **Scala**: 2.13.18
- **sbt**: 1.10.8 (managed by wrapper script)

### Common Build Commands

```bash
# Start sbt console
./sbt

# Compile all modules
./sbt compile

# Run tests for a specific module
./sbt "project http-server" test

# Run a specific test
./sbt "project http-server" "testOnly com.twitter.finatra.http.tests.routing.RoutesTest"

# Run all tests (note: this can take a while)
./sbt test

# Create assembly jar for examples
./sbt "project scalaHttpServer" assembly

# Run benchmarks
./sbt "project benchmarks" "jmh:run -i 10 -wi 20 -f1 -t1 .*JsonBenchmark.*"
```

### Project Structure

The repository is organized into multiple sbt modules:

**Inject Framework** (dependency injection layer):
- `inject-core` - Core dependency injection utilities
- `inject-app` - Injectable application framework
- `inject-server` - Injectable server framework
- `inject-modules` - Common Guice modules
- `inject-thrift`, `inject-thrift-client` - Thrift integration

**HTTP Framework**:
- `http-core` - Core HTTP utilities (exceptions, marshalling, file upload)
- `http-server` - HTTP server implementation
- `http-client` - HTTP client utilities
- `http-mustache` - Mustache template support
- `http-annotations` - HTTP annotations

**Thrift Framework**:
- `thrift` - Thrift server implementation

**Other**:
- `jackson` - JSON serialization/deserialization
- `validation` - Request validation
- `utils` - Common utilities
- `benchmarks` - Performance benchmarks

**Examples**: Located in `examples/` directory with working HTTP and Thrift server examples.

## Architecture

### HTTP Server Pattern

HTTP servers extend `HttpServer` trait and implement `configureHttp`:

```scala
class ExampleServer extends HttpServer {
  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[ExampleController]
  }
}
```

Controllers extend `Controller` and use the RouteDSL:

```scala
class ExampleController extends Controller {
  get("/endpoint") { request: Request =>
    response.ok.json(Map("message" -> "hello"))
  }
}
```

### Thrift Server Pattern

Thrift servers extend `ThriftServer` trait and implement `configureThrift`:

```scala
class ExampleServer extends ThriftServer {
  override def configureThrift(router: ThriftRouter): Unit = {
    router.add[ExampleThriftController]
  }
}
```

### Dependency Injection

Finatra uses Google Guice for dependency injection. Modules extend `TwitterModule`:

```scala
object MyModule extends TwitterModule {
  @Provides
  @Singleton
  def providesMyService(): MyService = new MyServiceImpl()
}
```

Add modules in server configuration:

```scala
override val modules = Seq(MyModule)
```

### Testing

Tests extend `Test` trait (which extends ScalaTest's `AnyFunSuite` with `TestMixin`):

```scala
class MyFeatureTest extends Test {
  test("my feature works") {
    // test code
  }
}
```

For integration/feature tests, use `EmbeddedHttpServer` or `EmbeddedThriftServer`:

```scala
val server = new EmbeddedHttpServer(new ExampleServer)
server.httpGet("/endpoint", andExpect = Status.Ok)
```

## Development Workflow

### Running Tests

Tests fork by default (configured in `build.sbt`). ScalaTest is used with the FunSuite style.

To run a single test class:
```bash
./sbt "project <module-name>" "testOnly <fully.qualified.TestClass>"
```

To run tests matching a pattern:
```bash
./sbt "project http-server" "testOnly *RoutesTest"
```

### Working with Examples

Examples are located in `examples/` and are part of the sbt build:

```bash
# Run an example HTTP server
./sbt "project scalaHttpServer" run

# Run tests for an example
./sbt "project scalaHttpServer" test
```


## Code Style

- Follow the Scala Style Guide
- Tests use ScalaTest FunSuite with Matchers
- Use `com.twitter.inject.Test` as the base test trait
- Commit messages should follow the format: `module-name: Short description`
- Update CHANGELOG.rst for API changes, new features, and bug fixes

## Important Notes

- The develop branch is the main development branch (not master)
- Pull requests should target the `develop` branch
- This is a Twitter OSS project with an internal mirror - PRs are reviewed then synced internally
- Many modules publish test jars for reusable test utilities
- Default ports: HTTP :8888, HTTPS (empty/disabled), Thrift :9999
