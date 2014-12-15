package com.twitter.finatra

import java.util.concurrent.Callable

import com.twitter.mustache.ScalaObjectHandler
import com.twitter.util.Future

class TwitterObjectHandler extends ScalaObjectHandler {

      override def coerce(value: Object) = {
        value match {
          case f: Future[_] => {
              new Callable[Any]() {
                  def call() = {
                      val value = f.get().asInstanceOf[Object]
                      coerce(value)
                    }
                }
            }
          case _ => super.coerce(value)
        }
      }
  }

