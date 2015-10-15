package com.github.dtaniwaki.akka_pusher

object PusherExceptions {
  class PusherException(message: String) extends RuntimeException(message)
  class BadRequest(message: String) extends PusherException(message)
  class Unauthorized(message: String) extends PusherException(message)
  class Forbidden(message: String) extends PusherException(message)
}
