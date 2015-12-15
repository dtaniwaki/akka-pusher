package com.github.dtaniwaki.akka_pusher

import spray.json._
import spray.json.DefaultJsonProtocol._

object PusherEvents {
  sealed abstract class PusherEvent

  case class ChannelOccupiedEvent(
    name: String,
    channel: String) extends PusherEvent
  object ChannelOccupiedEvent {
    implicit val ChannelOccupiedEventJsonSupport = jsonFormat(ChannelOccupiedEvent.apply _, "name", "channel")
  }

  case class ChannelVacatedEvent(
    name: String,
    channel: String) extends PusherEvent
  object ChannelVacatedEvent {
    implicit val ChannelVacatedEventJsonSupport = jsonFormat(ChannelVacatedEvent.apply _, "name", "channel")
  }

  case class MemberAddedEvent(
    name: String,
    channel: String,
    userId: String) extends PusherEvent
  object MemberAddedEvent {
    implicit val MemberAddedEventJsonSupport = jsonFormat(MemberAddedEvent.apply _, "name", "channel", "user_id")
  }

  case class MemberRemovedEvent(
    name: String,
    channel: String,
    userId: String) extends PusherEvent
  object MemberRemovedEvent {
    implicit val MemberRemovedEventJsonSupport = jsonFormat(MemberRemovedEvent.apply _, "name", "channel", "user_id")
  }

  case class ClientEvent(
    name: String,
    channel: String,
    userId: String,
    event: String,
    data: Map[String, String],
    socketId: String) extends PusherEvent
  object ClientEvent {
    implicit object ClientEventJsonSupport extends JsonFormat[ClientEvent] {
      def write(event: ClientEvent): JsValue = {
        JsObject(
          "name" -> JsString(event.name),
          "channel" -> JsString(event.channel),
          "user_id" -> JsString(event.userId),
          "event" -> JsString(event.event),
          "data" -> JsString(event.data.toJson.compactPrint),
          "socket_id" -> JsString(event.socketId)
        )
      }

      def read(json: JsValue): ClientEvent = {
        json.asJsObject.getFields("name", "channel", "user_id", "event", "data", "socket_id") match {
          case Seq(JsString(name), JsString(channel), JsString(userId), JsString(event), JsString(data), JsString(socketId)) =>
            ClientEvent(name, channel, userId, event, data.parseJson.convertTo[Map[String, String]], socketId)
        }
      }
    }
  }
}
