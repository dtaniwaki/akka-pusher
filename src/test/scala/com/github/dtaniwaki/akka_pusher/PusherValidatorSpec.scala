package com.github.dtaniwaki.akka_pusher

import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution

class PusherValidatorSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with PusherValidator {
  "#validateChannel(String)" should {
    "be valid" in {
      val channel = "channel"

      {
        validateChannel(channel)
      } must not(throwA[Exception])
    }
    "200 length" in {
      "be valid" in {
        val channel = (for (n <- 1 to 200) yield ("a")).mkString

        {
          validateChannel(channel)
        } must not(throwA[Exception])
      }
    }
    "more than 200 length" in {
      "be invalid" in {
        val channel = (for (n <- 1 to 201) yield ("a")).mkString

        {
          validateChannel(channel)
        } must throwA(new IllegalArgumentException(s"requirement failed: The channel is too long: $channel"))
      }
    }
    "with invalid characters" in {
      "be invalid" in {
        val channel = "channel?"

        {
          validateChannel(channel)
        } must throwA(new IllegalArgumentException(s"requirement failed: The channel is invalid: $channel"))
      }
    }
  }
  "#validateChannel(Seq[String])" should {
    "be valid" in {
      val channels = Seq("channel1", "channel2")

      {
        validateChannel(channels)
      } must not(throwA[Exception])
    }
    "200 length" in {
      "be valid" in {
        val channels = Seq("channel1", (for (n <- 1 to 200) yield ("a")).mkString)

        {
          validateChannel(channels)
        } must not(throwA[Exception])
      }
    }
    "more than 200 length" in {
      "be invalid" in {
        val channels = Seq("channel1", (for (n <- 1 to 201) yield ("a")).mkString)

        {
          validateChannel(channels)
        } must throwA(new IllegalArgumentException(s"requirement failed: The channel is too long: ${channels(1)}"))
      }
    }
    "with invalid characters" in {
      "be invalid" in {
        val channels = Seq("channel1", "channel?")

        {
          validateChannel(channels)
        } must throwA(new IllegalArgumentException(s"requirement failed: The channel is invalid: ${channels(1)}"))
      }
    }
  }
  "#validateSocketId(String)" should {
    "be valid" in {
      val socketId = "123.234"

      {
        validateSocketId(socketId)
      } must not(throwA[Exception])
    }
    "with invalid characters" in {
      "be invalid" in {
        val socketId = "socket"

        {
          validateSocketId(socketId)
        } must throwA(new IllegalArgumentException(s"requirement failed: The socketId is invalid: $socketId"))
      }
    }
  }
  "#validateSocketId(Option[String])" should {
    "be valid" in {
      val maybeSocketId = Some("123.234")

      {
        validateSocketId(maybeSocketId)
      } must not(throwA[Exception])
    }
    "with invalid characters" in {
      "be invalid" in {
        val maybeSocketId = Some("socket")

        {
          validateSocketId(maybeSocketId)
        } must throwA(new IllegalArgumentException(s"requirement failed: The socketId is invalid: ${maybeSocketId.get}"))
      }
    }
    "with None" in {
      "be valid" in {
        val maybeSocketId = None

        {
          validateSocketId(maybeSocketId)
        } must not(throwA[Exception])
      }
    }
  }
  "#validateTriggers" should {
    "be valid" in {
      val triggers = Seq(
        ("channel1", "event1", "message1", Some("123.234")),
        ("channel2", "event2", "message2", Some("234.345"))
      )

      {
        validateTriggers(triggers)
      } must not(throwA[Exception])
    }
    "with 100 triggers" in {
      "be invalid" in {
        val triggers = for (n <- 1 to 100) yield ("channel1", "event1", "message1", Some("123.234"))

        {
          validateTriggers(triggers)
        } must not(throwA[Exception])
      }
    }
    "with more than 100 triggers" in {
      "be invalid" in {
        val triggers = for (n <- 1 to 101) yield ("channel1", "event1", "message1", Some("123.234"))

        {
          validateTriggers(triggers)
        } must throwA(new IllegalArgumentException(s"requirement failed: The length of the triggers is too many: 101"))
      }
    }
    "with invalid channel" in {
      "be valid" in {
        val triggers = Seq(
          ("channel1", "event1", "message1", Some("123.234")),
          ("channel?", "event2", "message2", Some("234.345"))
        )

        {
          validateTriggers(triggers)
        } must throwA(new IllegalArgumentException(s"requirement failed: The channel is invalid: channel?"))
      }
    }
    "with invalid socketId" in {
      "be valid" in {
        val triggers = Seq(
          ("channel1", "event1", "message1", Some("123.234")),
          ("channel2", "event2", "message2", Some("socket"))
        )

        {
          validateTriggers(triggers)
        } must throwA(new IllegalArgumentException(s"requirement failed: The socketId is invalid: socket"))
      }
    }
  }
}
