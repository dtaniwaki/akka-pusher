package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherEvents._
import com.github.dtaniwaki.akka_pusher.PusherRequests._
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import com.github.nscala_time.time.Imports._
import spray.json._
import spray.json.DefaultJsonProtocol._

class PusherRequestsSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution {

  "WebhookRequestJsonSupport" should {
    "with multiple different events" in {
      "read from json object" in {
        val data: Map[String, String] = Map("foo" -> "bar")
        val event1 = ClientEvent(name = "client_event", channel = "test", userId = "123", data = data, event = "event", socketId = "123.234")
        val event2 = ChannelOccupiedEvent("channel_occupied", "test")
        val dataString = data.toJson.toString.replace("\"", "\\\"")
        s"""{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": "$dataString", "event": "event", "socket_id": "123.234"},{"name":"channel_occupied","channel":"test"}]}"""
          .parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event1, event2))
      }
      "write to json object" in {
        val data: Map[String, String] = Map("foo" -> "bar")
        val event1 = ClientEvent(name = "client_event", channel = "test", userId = "123", data = data, event = "event", socketId = "123.234")
        val event2 = ChannelOccupiedEvent("channel_occupied", "test")
        val dataString = data.toJson.toString.replace("\"", "\\\"")
        WebhookRequest(new DateTime(12345000), List(event1, event2)).toJson === s"""{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": "$dataString", "event": "event", "socket_id": "123.234"},{"name":"channel_occupied","channel":"test"}]}""".parseJson
      }
    }
    "with invalid event" in {
      "does not read from json object" in {
        """{"time_ms": 12345, "events":[{"name":"invalid_event", "channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List())
      }
      "does not read from json object without name" in {
        """{"time_ms": 12345, "events":[{"channel": "test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List())
      }
    }
    "with client event" in {
      "read from json object" in {
        val data: Map[String, String] = Map("foo" -> "bar")
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = data, event = "event", socketId = "123.234")
        val dataString = data.toJson.toString.replace("\"", "\\\"")
        s"""{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": "$dataString", "event": "event", "socket_id": "123.234"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val data: Map[String, String] = Map("foo" -> "bar")
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = data, event = "event", socketId = "123.234")
        val dataString = data.toJson.toString.replace("\"", "\\\"")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === s"""{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": "$dataString", "event": "event", "socket_id": "123.234"}]}""".parseJson
      }
    }
    "with channel_occupied event" in {
      "read from json object" in {
        val event = ChannelOccupiedEvent("channel_occupied", "test")
        """{"time_ms": 12345, "events":[{"name":"channel_occupied", "channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = ChannelOccupiedEvent("channel_occupied", "test")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name":"channel_occupied", "channel":"test"}]}""".parseJson
      }
    }
    "with channel_vacated event" in {
      "read from json object" in {
        val event = ChannelVacatedEvent("channel_vacated", "test")
        """{"time_ms": 12345, "events":[{"name":"channel_vacated", "channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = ChannelVacatedEvent("channel_vacated", "test")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name":"channel_vacated", "channel":"test"}]}""".parseJson
      }
    }
    "with member_added event" in {
      "read from json object" in {
        val event = MemberAddedEvent("member_added", "test", "foo")
        """{"time_ms": 12345, "events":[{"name":"member_added", "channel":"test", "user_id":"foo"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = MemberAddedEvent("member_added", "test", "foo")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name":"member_added", "channel":"test", "user_id":"foo"}]}""".parseJson
      }
    }
    "with member_removed event" in {
      "read from json object" in {
        val event = MemberRemovedEvent("member_removed", "test", "foo")
        """{"time_ms": 12345, "events":[{"name":"member_removed", "channel":"test", "user_id":"foo"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = MemberRemovedEvent("member_removed", "test", "foo")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name":"member_removed", "channel":"test", "user_id":"foo"}]}""".parseJson
      }
    }
    // TODO: Add specs for all the events individually
  }
}
