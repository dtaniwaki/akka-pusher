package com.github.dtaniwaki.akka_pusher

object PusherExceptions {
  class PusherException(message: String) extends RuntimeException(message)
}
