package com.github.dtaniwaki.akka_pusher

object PusherModels {
  case class Channels(
    data: String
  )
  case class Channel(
    data: String
  )
  case class Users(
    data: String
  )
  case class Result(
    data: String
  )
  case class AuthenticatedParams(
    auth: String,
    channelData: Option[String] = None
  )
}
