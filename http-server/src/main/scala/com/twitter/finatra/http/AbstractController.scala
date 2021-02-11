package com.twitter.finatra.http

/** [[com.twitter.finatra.http.Controller]] for Java Compatibility */
abstract class AbstractController extends Controller {

  /**
   * Java users should create and configure routes inside of this method. E.g.
   *
   * @example
   * {{{
   *
   * public class MyController extends AbstractController {
   *   private final AService aService;
   *
   *   @Inject
   *   public MyController(
   *     AService aService,
   *     @Flag("cool.flag") String coolFlagValue
   *   ) {
   *     this.aService = aService;
   *   }
   *
   *   public void configureRoutes() {
   *
   *     get("/foo", (Request request) -> Future.value("hello"));
   *
   *     post("/baz", aService::doBaz);
   *
   *     put("/bar", (Request request) -> aService.doBar(request, coolFlagValue));
   *   }
   * }
   * }}}
   */
  def configureRoutes(): Unit
}
