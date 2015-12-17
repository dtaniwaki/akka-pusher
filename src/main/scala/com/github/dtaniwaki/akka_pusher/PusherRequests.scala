package com.github.dtaniwaki.akka_pusher

import com.github.nscala_time.time.Imports._
import PusherEvents._
import spray.json._

object PusherRequests extends PusherJsonSupport {
  case class AuthRequest(
    socketId: String,
    channelName: String)
  object AuthRequest {
    implicit val AuthRequestJsonSupport = jsonFormat(AuthRequest.apply _, "socket_id", "channel_name")
  }

  case class WebhookRequest(
    timeMs: DateTime,
    events: Seq[PusherEvent])
  object WebhookRequest {
    implicit object WebhookRequestJsonSupport extends RootJsonFormat[WebhookRequest] {
      def write(res: WebhookRequest): JsValue = {
        val events = res.events.map {
          case event: ChannelOccupiedEvent => event.toJson
          case event: ChannelVacatedEvent  => event.toJson
          case event: MemberAddedEvent     => event.toJson
          case event: MemberRemovedEvent   => event.toJson
          case event: ClientEvent          => event.toJson
        }.toVector
        JsObject(
          "time_ms" -> JsNumber((res.timeMs.getMillis / 1000).toLong),
          "events" -> JsArray(events)
        )
      }
      def read(json: JsValue): WebhookRequest = {
        json.asJsObject.getFields("time_ms", "events") match {
          case Seq(JsNumber(timeMs), JsArray(events)) =>
            WebhookRequest(new DateTime(timeMs.toLong * 1000), events.map { event =>
              event.asJsObject.getFields("name") match {
                case Seq(JsString("client_event"))     => Some(event.convertTo[ClientEvent])
                case Seq(JsString("channel_occupied")) => Some(event.convertTo[ChannelOccupiedEvent])
                case Seq(JsString("channel_vacated"))  => Some(event.convertTo[ChannelVacatedEvent])
                case Seq(JsString("member_added"))     => Some(event.convertTo[MemberAddedEvent])
                case Seq(JsString("member_removed"))   => Some(event.convertTo[MemberRemovedEvent])
                case _                                 => None
              }
            }.filter(_.isDefined).map(_.get))
        }
      }
    }
  }
}
