package com.github.dtaniwaki.akka_pusher

import spray.json._
import com.github.nscala_time.time.Imports._
import org.joda.time.format._

trait PusherJsonSupport extends DefaultJsonProtocol {
  import PusherRequests._
  import PusherModels._
  import PusherEvents._

  private implicit object DateTimeJsonFormat extends RootJsonFormat[DateTime] {
    private lazy val format = ISODateTimeFormat.dateTimeNoMillis()

    def write(datetime: DateTime): JsValue = JsString(format.print(datetime.withZone(DateTimeZone.UTC)))

    def read(json: JsValue): DateTime = json match {
      case JsString(x) => format.parseDateTime(x)
      case x           => deserializationError("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit val ChannelOccupiedEventJsonSupport = jsonFormat(ChannelOccupiedEvent.apply, "name", "channel")
  implicit val ChannelVacatedEventJsonSupport = jsonFormat(ChannelVacatedEvent.apply, "name", "channel")
  implicit val MemberAddedEventJsonSupport = jsonFormat(MemberAddedEvent.apply, "name", "channel", "user_id")
  implicit val MemberRemovedEventJsonSupport = jsonFormat(MemberRemovedEvent.apply, "name", "channel", "user_id")

  implicit object ClientEventJsonSupport extends RootJsonFormat[ClientEvent] {
    def write(res: ClientEvent): JsValue = {
      res.toJson // FIXME: Map to the correct name
    }
    def read(json: JsValue): ClientEvent = {
      val event = json.convertTo[Map[String, String]]
      val data = event.getOrElse("data", "{}").toJson.convertTo[Map[String, String]]
      ClientEvent(event.getOrElse("name", ""), event.getOrElse("channel", ""), event.getOrElse("user_id", ""), event.getOrElse("event", ""), data, event.getOrElse("socket_id", ""))
    }
  }

  implicit object ChannelDataJsonSupport extends RootJsonFormat[PresenceChannelData] {
    override def write(data: PresenceChannelData): JsValue =
      data.userInfo.map { userInfo =>
        JsObject(
          "user_id" -> JsString(data.userId),
          "user_info" -> userInfo.toJson
        )
      }.getOrElse(
        JsObject("user_id" -> JsString(data.userId))
      )


    override def read(json: JsValue): PresenceChannelData =
      json.asJsObject.getFields("user_id", "user_info") match {
        case Seq(JsString(userId), userInfo) =>
          PresenceChannelData(userId, Some(userInfo.convertTo[Map[String, String]]))
        case Seq(JsString(userId), JsNull) =>
          PresenceChannelData(userId, None)
        case x => deserializationError("ChannelData is expected: " + x)
      }
  }

  implicit val AuthRequestJsonSupport = jsonFormat(AuthRequest.apply, "socket_id", "channel_name")
  implicit object WebhookRequestJsonSupport extends RootJsonFormat[WebhookRequest] {
    def write(res: WebhookRequest): JsValue = {
      res.toJson
    }
    def read(json: JsValue): WebhookRequest = {
      json.asJsObject.getFields("time_ms", "events") match {
        case Seq(JsString(timeMs), JsArray(events)) =>
          WebhookRequest(timeMs.toJson.convertTo[DateTime], events.map { event =>
            val pattern = "^client-".r
            event.toJson.convertTo[Map[String, String]].getOrElse("name", "") match {
              case pattern(s)         => event.convertTo[ClientEvent]
              case "channel-occupied" => event.convertTo[ChannelOccupiedEvent]
              case "channel-vacated"  => event.convertTo[ChannelVacatedEvent]
              case "member-added"     => event.convertTo[MemberAddedEvent]
              case "member-removed"   => event.convertTo[MemberRemovedEvent]
            }
          })
        case x => deserializationError("WebhookRequest is expected: " + x)
      }
    }
  }

  implicit val ResultJsonSupport = jsonFormat1(Result)
  implicit val ChannelsJsonSupport = jsonFormat1(Channels)
  implicit val ChannelJsonSupport = jsonFormat1(Channel)
  implicit val UsersJsonSupport = jsonFormat1(Users)
  implicit val AuthenticatedParamsJsonSupport = jsonFormat(AuthenticatedParams.apply, "auth", "channel_data")
}
