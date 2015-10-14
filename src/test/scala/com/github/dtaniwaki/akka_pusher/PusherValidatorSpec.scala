package com.github.dtaniwaki.akka_pusher

import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution

import PusherExceptions._

class PusherValidatorSpec extends Specification
  with RandomSequentialExecution
  with PusherValidator
{
  "#validateChannel" should {
    "be valid" in {
      val channel = "channel"

      validateChannel(channel) === true
    }
    "200 length" in {
      "be valid" in {
        val channel = (for (n <- 1 to 200) yield("a")).mkString

        validateChannel(channel) === true
      }
    }
    "more than 200 length" in {
      "be invalid" in {
        val channel = (for (n <- 1 to 201) yield("a")).mkString

        {
          validateChannel(channel)
        } must throwA(new PusherException(s"The channel is too long: $channel"))
      }
    }
    "with invalid characters" in {
      "be invalid" in {
        val channel = "channel?"

        {
          validateChannel(channel)
        } must throwA(new PusherException(s"The channel is invalid: $channel"))
      }
    }
  }
  "#validateSocketId" should {
    "be valid" in {
      val socketId = "123.234"

      validateSocketId(socketId)
    }
    "with invalid characters" in {
      "be invalid" in {
        val socketId = "socket"

        {
          validateSocketId(socketId)
        } must throwA(new PusherException(s"The socketId is invalid: $socketId"))
      }
    }
  }
}
