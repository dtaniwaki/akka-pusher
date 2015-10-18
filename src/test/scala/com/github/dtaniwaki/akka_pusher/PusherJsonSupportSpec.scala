package com.github.dtaniwaki.akka_pusher

import spray.json._
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.joda.time.format._
import com.github.nscala_time.time.Imports._

import PusherRequests._
import PusherEvents._
import PusherModels._

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
        """{"time_ms": 12345, "events":[{"name":"client_event", "channel":"test", "user_id":"123", "data": {"foo":"bar"}, "event":"event", "socket_id":"123.234"},{"name":"channel_occupied","channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event1, event2))
      }
      "write to json object" in {
        val event1 = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        val event2 = ChannelOccupiedEvent("channel_occupied", "test")
        WebhookRequest(new DateTime(12345000), List(event1, event2)).toJson === """{"time_ms": 12345, "events":[{"name":"client_event", "channel":"test", "user_id":"123", "data": {"foo":"bar"}, "event":"event", "socket_id":"123.234"},{"name":"channel_occupied","channel":"test"}]}""".parseJson
      }
    }
    "with invalid event" in {
      "does not read from json object" in {
        """{"time_ms": 12345, "events":[{"name":"invalid_event", "channel":"test"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List())
      }
    }
    "with client event" in {
      "read from json object" in {
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        """{"time_ms": 12345, "events":[{"name":"client_event", "channel":"test", "user_id":"123", "data": {"foo":"bar"}, "event":"event", "socket_id":"123.234"}]}""".parseJson.convertTo[WebhookRequest] === WebhookRequest(new DateTime(12345000), List(event))
      }
      "write to json object" in {
        val event = ClientEvent(name = "client_event", channel = "test", userId = "123", data = Map("foo" -> "bar"), event = "event", socketId = "123.234")
        WebhookRequest(new DateTime(12345000), List(event)).toJson === """{"time_ms": 12345, "events":[{"name":"client_event", "channel":"test", "user_id":"123", "data": {"foo":"bar"}, "event":"event", "socket_id":"123.234"}]}""".parseJson
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
    // TODO: Add specs for all the events individually
  }
  "ChannelMapJsonFormat" should {
    "with channels" in {
      "read from json object" in {
        val channels = Map(
          "channel1" -> Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2)),
          "channel2" -> Channel(occupied = Some(true), userCount = Some(2), subscriptionCount = Some(3))
        )
        """{"channel1": {"occupied": true, "user_count": 1, "subscription_count": 2}, "channel2": {"occupied": true, "user_count": 2, "subscription_count": 3}}""".parseJson.convertTo[Map[String, Channel]] === channels
      }
      "write to json object" in {
        val channels = Map(
          "channel1" -> Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2)),
          "channel2" -> Channel(occupied = Some(true), userCount = Some(2), subscriptionCount = Some(3))
        )
        channels.toJson === """{"channel1": {"occupied": true, "user_count": 1, "subscription_count": 2}, "channel2": {"occupied": true, "user_count": 2, "subscription_count": 3}}""".parseJson
      }
    }
    "without channels" in {
      "read from json object" in {
        val channels = Map[String, Channel]()
        """{}""".parseJson.convertTo[Map[String, Channel]] === channels
      }
      "write to json object" in {
        val channels = Map[String, Channel]()
        channels.toJson === """{}""".parseJson
      }
    }
  }
  "ChannelJsonFormat" should {
    "with all fields" in {
      "read from json object" in {
        val channel = Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2))
        """{"occupied": true, "user_count": 1, "subscription_count": 2}""".parseJson.convertTo[Channel] === channel
      }
      "write to json object" in {
        val channel = Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2))
        channel.toJson === """{"occupied": true, "user_count": 1, "subscription_count": 2}""".parseJson
      }
    }
  }
  "UserListJsonFormat" should {
    "with users" in {
      "read from json object" in {
        val users = List[User](User("123"), User("234"))
        """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson.convertTo[List[User]] === users
      }
      "write to json object" in {
        val users = List[User](User("123"), User("234"))
        users.toJson === """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson
      }
    }
    "without users" in {
      "read from json object" in {
        val users = List[User]()
        """{"users": []}""".parseJson.convertTo[List[User]] === users
      }
      "write to json object" in {
        val users = List[User]()
        users.toJson === """{"users": []}""".parseJson
      }
    }
  }
  "UserJsonFormat" should {
    "with all fields" in {
      "read from json object" in {
        val user = User("123")
        """{"id": "123"}""".parseJson.convertTo[User] === user
      }
      "write to json object" in {
        val user = User("123")
        user.toJson === """{"id": "123"}""".parseJson
      }
    }
  }
}
