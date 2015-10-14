package com.github.dtaniwaki.akka_pusher

import PusherExceptions._

trait PusherValidator {
  private val channelPattern = """^([A-Za-z0-9_\-=@,.;]+)$""".r
  private val socketIdPattern = """^(\d+\.\d+)$""".r

  def validateChannel(channel: String): Boolean = {
    if (200 < channel.length) {
      throw new PusherException(s"The channel is too long: $channel")
    }
    channel match {
      case channelPattern(_) =>
        return true
      case _ =>
        throw new PusherException(s"The channel is invalid: $channel")
    }
    return false
  }

  def validateSocketId(socketId: String): Boolean = {
    socketId match {
      case socketIdPattern(_) =>
        return true
      case _ =>
        throw new PusherException(s"The socketId is invalid: $socketId")
    }
    return false
  }
}
