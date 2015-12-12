package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels.ChannelData
import spray.json.JsValue

object PusherMessages {
  case class TriggerMessage(
    channel: String,
    event: String,
    message: JsValue,
    socketId: Option[String] = None)
  @deprecated("TriggerMessage will be used for BatchTriggerMessage", "0.3")
  case class BatchTriggerMessage(
    channel: String,
    event: String,
    message: JsValue,
    socketId: Option[String] = None)
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
  case class BatchTriggerTick()
}
