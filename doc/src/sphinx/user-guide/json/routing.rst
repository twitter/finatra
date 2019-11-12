.. _json_routing:

JSON Integration with HTTP Routing
==================================

Requests
--------

When using a `custom "request" case class <../http/requests.html#custom-case-class-request-object>`__ as a route callback's input type, Finatra will parse the incoming request body into the case class using the server's configured `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__.

Similar to declaratively parsing a `GET` request (described above), Finatra will perform validations and return a `400 BadRequest` with a list of all accumulated errors in JSON format.

Suppose you wanted to handle a `POST` of the following JSON (representing a group of tweet ids):

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


This case class could then used as the input to a Controller route callback:

.. code:: scala

    post("/tweets") { request: GroupRequest =>
      ...
    }

Responses
---------

To return a JSON response you can simply return a `case class` as the return of your route callback. The default behavior will be to render this `case class` as a JSON response.

See the `HTTP Responses <../http/responses.html>`__ section for more information.
