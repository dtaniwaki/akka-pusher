package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels.ChannelData
import spray.json.JsValue

object PusherMessages {
  case class TriggerMessage(
    channels: Seq[String],
    event: String,
    message: JsValue,
    socketId: Option[String] = None)
  object TriggerMessage {
    def apply(channel: String, event: String, message: JsValue): TriggerMessage = apply(channel, event, message, None)
    def apply(channel: String, event: String, message: JsValue, socketId: Option[String]): TriggerMessage =
      new TriggerMessage(Seq(channel), event, message, socketId)
  }
  case class ChannelMessage(
    channel: String,
    attributes: Option[Seq[String]] = None)
  case class ChannelsMessage(
    prefixFilter: String,
    attributes: Option[Seq[String]] = None)
  case class UsersMessage(
    channel: String)
  case class AuthenticateMessage(
    socketId: String,
    channel: String,
    data: Option[ChannelData[JsValue]] = None)
  case class ValidateSignatureMessage(
    key: String,
    signature: String,
    body: String)
  case class ResponseMessage(
    message: Any)
}
