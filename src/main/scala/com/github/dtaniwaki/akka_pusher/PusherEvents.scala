package com.github.dtaniwaki.akka_pusher

object PusherEvents {
  sealed abstract class PusherEvent {
    val name: String
    val channel: String
  }

  case class ChannelOccupiedEvent(
    name: String,
    channel: String) extends PusherEvent

  case class ChannelVacatedEvent(
    name: String,
    channel: String) extends PusherEvent

  case class MemberAddedEvent(
    name: String,
    channel: String,
    userId: String) extends PusherEvent

  case class MemberRemovedEvent(
    name: String,
    channel: String,
    userId: String) extends PusherEvent

  case class ClientEvent(
    name: String,
    channel: String,
    userId: String,
    event: String,
    data: Map[String, String],
    socketId: String) extends PusherEvent

}
