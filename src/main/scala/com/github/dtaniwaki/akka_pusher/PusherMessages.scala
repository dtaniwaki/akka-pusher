package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels.{ ChannelData, PusherMsg }
import com.github.dtaniwaki.akka_pusher.attributes.{ PusherChannelAttributes, PusherChannelsAttributes }
import io.circe.Json

object PusherMessages {
  case class TriggerMessage(
    channel: String,
    event: String,
    message: String,
    socketId: Option[String] = None)
  @deprecated("TriggerMessage will be used for BatchTriggerMessage. It will be removed in v0.3", "0.2.3")
  case class BatchTriggerMessage(
    channel: String,
    event: String,
    message: String,
    socketId: Option[String] = None)
  case class ChannelMessage(
    channelName: String,
    attributes: Seq[PusherChannelAttributes.Value] = Seq())
  object ChannelMessage {
    @deprecated("Set the attributes without option and make it PusherChannelAttributes enumeration sequence instead. It will be removed in v0.3", "0.2.3")
    def apply(channel: String, attributes: Option[Seq[String]]): ChannelMessage = {
      new ChannelMessage(channel, attributes.getOrElse(Seq()).map(PusherChannelAttributes.withName(_)))
    }
  }
  case class ChannelsMessage(
    prefixFilter: String,
    attributes: Seq[PusherChannelsAttributes.Value] = Seq())
  object ChannelsMessage {
    @deprecated("Set the attributes without option and make it PusherChannelsAttributes enumeration sequence instead. It will be removed in v0.3", "0.2.3")
    def apply(prefixFilter: String, attributes: Option[Seq[String]]): ChannelsMessage = {
      new ChannelsMessage(prefixFilter, attributes.getOrElse(Seq()).map(PusherChannelsAttributes.withName(_)))
    }
  }
  case class UsersMessage(
    channel: String)
  case class AuthenticateMessage(
    socketId: String,
    channel: String,
    data: Option[ChannelData[PusherMsg]] = None)
  case class ValidateSignatureMessage(
    key: String,
    signature: String,
    body: String)
  case class BatchTriggerTick()
}
