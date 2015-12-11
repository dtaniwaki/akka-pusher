package com.github.dtaniwaki.akka_pusher

import spray.json.JsonWriter

trait PusherValidator {
  private val channelPattern = """^([A-Za-z0-9_\-=@,.;]+)$""".r
  private val socketIdPattern = """^(\d+\.\d+)$""".r

  def validateChannel(channel: String): Unit = {
    require(channel.length <= 200, s"The channel is too long: $channel")
    require(channelPattern.findFirstIn(channel).isDefined, s"The channel is invalid: $channel")
  }
  def validateChannel(channels: Seq[String]): Unit = channels.foreach(validateChannel)

  def validateSocketId(socketId: String): Unit = {
    require(socketIdPattern.findFirstIn(socketId).isDefined, s"The socketId is invalid: $socketId")
  }
  def validateSocketId(socketId: Option[String]): Unit = socketId.map(validateSocketId)

  def validateTriggers[T](triggers: Seq[(String, String, T, Option[String])]): Unit = {
    require(triggers.length <= 100, s"The length of the triggers is too many: ${triggers.length}")
    triggers.foreach {
      case (channel, eventName, data, socketId) =>
        validateChannel(channel)
        validateSocketId(socketId)
    }
  }
}
