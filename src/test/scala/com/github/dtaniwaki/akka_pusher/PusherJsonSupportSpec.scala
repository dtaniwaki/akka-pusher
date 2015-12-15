package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels.{ User, Channel }
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json._

class PusherJsonSupportSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with PusherJsonSupport {

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
}
