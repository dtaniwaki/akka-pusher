package com.github.dtaniwaki.akka_pusher

import akka.actor._
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global

class PusherActor extends Actor with StrictLogging {
  implicit val system = ActorSystem("pusher")

  val pusher = new PusherClient()

  override def receive: Receive = {
      case TriggerMessage(event, channel, message, socketId) =>
        pusher.trigger(event, channel, message, socketId).map(sender ! new ResponseMessage(_))
      case ChannelMessage(channel, attributes) =>
        pusher.channel(channel, attributes).map(sender ! new ResponseMessage(_))
      case ChannelsMessage(prefixFilter, attributes) =>
        pusher.channels(prefixFilter, attributes).map(sender ! new ResponseMessage(_))
      case UsersMessage(channel) =>
        pusher.users(channel).map(sender ! new ResponseMessage(_))
      case AuthenticateMessage(channel, socketId, data) =>
        sender ! new ResponseMessage(pusher.authenticate(channel, socketId, data))
      case ValidateSignatureMessage(key, signature, body) =>
        sender ! new ResponseMessage(pusher.validateSignature(key, signature, body))
      case message =>
        logger.info(s"Unknown event: $message")
  }

  override def postStop() = {
    super.postStop()
    pusher.shutdown()
  }
}

object PusherActor {
  def props(): Props = Props(new PusherActor())
}
