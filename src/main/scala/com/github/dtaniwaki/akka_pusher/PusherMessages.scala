package com.github.dtaniwaki.akka_pusher

object PusherMessages {
  case class TriggerMessage(
    channel: String,
    event: String,
    message: String,
    socketId: Option[String] = None
  )
  case class ChannelMessage(
    channel: String,
    attributes: Option[Seq[String]] = None
  )
  case class ChannelsMessage(
    prefixFilter: String,
    attributes: Option[Seq[String]] = None
  )
  case class UsersMessage(
    channel: String
  )
  case class AuthenticateMessage(
    socketId: String,
    channel: String,
    data: Option[Map[String, String]] = None
  )
  case class ValidateSignatureMessage(
    key: String,
    signature: String,
    body: String
  )
  case class ResponseMessage(
    message: Any
  )
}
