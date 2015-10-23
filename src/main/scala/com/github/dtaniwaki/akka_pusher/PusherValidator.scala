package com.github.dtaniwaki.akka_pusher

import PusherExceptions._

trait PusherValidator {
  private val channelPattern = """^([A-Za-z0-9_\-=@,.;]+)$""".r
  private val socketIdPattern = """^(\d+\.\d+)$""".r

  def validateChannel(channel: String): Unit = {
    require(channel.length <= 200, s"The channel is too long: $channel")
    require(channelPattern.findFirstIn(channel).isDefined, s"The channel is invalid: $channel")
  }

  def validateSocketId(socketId: String): Unit = {
    require(socketIdPattern.findFirstIn(socketId).isDefined, s"The socketId is invalid: $socketId")
  }
}
