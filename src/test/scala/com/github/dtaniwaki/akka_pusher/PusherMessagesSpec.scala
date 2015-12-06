package com.github.dtaniwaki.akka_pusher

import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import PusherMessages._
import spray.json._
import spray.json.DefaultJsonProtocol._

class PusherMessagesSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution {
  "#TriggerMessage(channel: String, event: String, data: JsValue)" should {
    "create a TriggerMessage" in {
      TriggerMessage("test", "event", "".toJson) === new TriggerMessage(Seq("test"), "event", "".toJson, None)
    }
  }
  "#TriggerMessage(channel: String, event: String, data: JsValue, socketId: Option[String])" should {
    "create a TriggerMessage" in {
      TriggerMessage("test", "event", "".toJson, Some("123.234")) === new TriggerMessage(Seq("test"), "event", "".toJson, Some("123.234"))
    }
  }
}
