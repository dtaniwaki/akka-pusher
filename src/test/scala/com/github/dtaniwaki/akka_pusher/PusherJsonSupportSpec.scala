package com.github.dtaniwaki.akka_pusher

import spray.json._
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.joda.time.format._
import com.github.nscala_time.time.Imports._

import PusherRequests._
import PusherEvents._

class PusherJsonSupportSpec extends Specification
  with SpecHelper
  with RandomSequentialExecution
  with PusherJsonSupport
{
  "WebhookRequestJsonSupport" should {
    "with multiple different events" in {
      "read from json object" in {
        val event1 = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        val event2 = ChannelOccupiedEvent("channel_occupied", "test")
        """{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": {"foo": "bar"}, "event": "event", "socket_id": "123.234"},{"name":"channel_occupied","channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event1, event2))
      }
      "write to json object" in {
        val event1 = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        val event2 = ChannelOccupiedEvent("channel_occupied", "test")
        WebhookRequest(new DateTime(12345000), List(event1, event2)).toJson === """{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": {"foo": "bar"}, "event": "event", "socket_id": "123.234"},{"name":"channel_occupied","channel":"test"}]}""".parseJson
      }
    }
    "with invalid event" in {
      "does not read from json object" in {
        """{"time_ms": 12345, "events":[{"name": "invalid_event", "channel": "test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List())
      }
    }
    "with client event" in {
      "read from json object" in {
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        """{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": {"foo": "bar"}, "event": "event", "socket_id": "123.234"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name": "client_event", "channel": "test", "user_id": "123", "data": {"foo": "bar"}, "event": "event", "socket_id": "123.234"}]}""".parseJson
      }
    }
    "with channel_occupied event" in {
      "read from json object" in {
        val event = ChannelOccupiedEvent("channel_occupied", "test")
        """{"time_ms": 12345, "events":[{"name": "channel_occupied", "channel": "test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = ChannelOccupiedEvent("channel_occupied", "test")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name": "channel_occupied", "channel": "test"}]}""".parseJson
      }
    }
    // TODO: Add specs for all the events individually
  }
}
