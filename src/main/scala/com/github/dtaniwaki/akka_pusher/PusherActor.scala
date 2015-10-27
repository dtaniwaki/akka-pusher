package com.github.dtaniwaki.akka_pusher

import akka.actor._
import akka.pattern.pipe
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.scalalogging.StrictLogging
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global

class PusherActor extends Actor with StrictLogging {
  implicit val system = ActorSystem("pusher")
  val pusher = new PusherClient()

  override def receive: Receive = {
    case TriggerMessageToChannels(channels, event, message, someSocketId) =>
      someSocketId.fold(
        sender ! new ResponseMessage(Await.result(pusher.trigger(channels, event, message), 5 seconds))
      ) { socketId =>
        sender ! new ResponseMessage(Await.result(pusher.trigger(channels, event, message, Some(socketId)), 5 seconds))
      }
    case TriggerMessage(channel, event, message, socketId) =>
      pusher.trigger(channel, event, message, socketId).map(new ResponseMessage(_)) pipeTo sender
    case ChannelMessage(channel, attributes) =>
      pusher.channel(channel, attributes).map(new ResponseMessage(_)) pipeTo sender
    case ChannelsMessage(prefixFilter, attributes) =>
      pusher.channels(prefixFilter, attributes).map(new ResponseMessage(_)) pipeTo sender
    case UsersMessage(channel) =>
      pusher.users(channel).map(new ResponseMessage(_)) pipeTo sender
    case AuthenticateMessage(channel, socketId, data) =>
      sender ! new ResponseMessage(pusher.authenticate(channel, socketId, data))
    case ValidateSignatureMessage(key, signature, body) =>
      sender ! new ResponseMessage(pusher.validateSignature(key, signature, body))
    case message =>
      logger.info(s"Unknown event: $message")
  }

  override def postStop(): Unit = {
    super.postStop()
    pusher.shutdown()
  }
}

object PusherActor {
  def props(): Props = Props(new PusherActor())
}
