.. _json_routing:

JSON Integration with Routing
=============================

If a `custom case class request object <../http/requests.html#custom-case-class-request-object>`__ is used as a route callback's input type, Finatra will parse the request body into the custom request class. Similar to declaratively parsing a `GET` request (described above), Finatra will perform validations and return a `400 BadRequest` with a list of all accumulated errors in JSON format.

Suppose you wanted to handle a POST of the following JSON (representing a group of tweet ids):

.. code:: json

    {
      "name": "EarlyTweets",
      "description": "Some of the earliest tweets on Twitter.",
      "tweetIds": [20, 22, 24],
      "dates": {
        "start": "1",
        "end": "2"
      }
    }


You could create and use the following case classes:

.. code:: scala

    case class GroupRequest(
      @NotEmpty name: String,
      description: Option[String],
      tweetIds: Set[Long],
      dates: Dates) {

      @MethodValidation
      def validateName = {
        ValidationResult.validate(
          name.startsWith("grp-"),
          "name must start with 'grp-'")
      }
    }

    case class Dates(
      @PastTime start: DateTime,
      @PastTime end: DateTime)


To return a JSON response you can simply return a `case class` as the return of your route callback. The default behavior will be to render this `case class` as a JSON response.

See the `HTTP Responses <../http/responses.html>`__ section for more information.
