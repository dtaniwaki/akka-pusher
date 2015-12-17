package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.github.dtaniwaki.akka_pusher.attributes.{ PusherChannelsAttributes, PusherChannelAttributes }
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json._

class PusherMessagesSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with PusherJsonSupport {

  "ChannelMessage" in {
    "#apply" should {
      "convert the argument type (deprecated)" in {
        ChannelMessage("channel", Some(Seq("user_count"))) === ChannelMessage("channel", Seq(PusherChannelAttributes.userCount))
      }
    }
  }
  "ChannelsMessage" in {
    "#apply" should {
      "convert the argument type (deprecated)" in {
        ChannelsMessage("channel", Some(Seq("user_count"))) === ChannelsMessage("channel", Seq(PusherChannelsAttributes.userCount))
      }
    }
  }
}
