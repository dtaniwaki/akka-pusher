package com.github.dtaniwaki.akka_pusher

import akka.actor._
import akka.pattern.pipe
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.scalalogging.StrictLogging
import spray.json.DefaultJsonProtocol._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PusherActor extends Actor with StrictLogging {
  implicit val system = ActorSystem("pusher")
  val pusher = new PusherClient()

  override def receive: Receive = PartialFunction { message =>
    val future: Future[Any] = try {
      message match {
        case TriggerMessage(channel, event, message, socketId) =>
          pusher.trigger(channel, event, message, socketId)
        case ChannelMessage(channel, attributes) =>
          pusher.channel(channel, attributes)
        case ChannelsMessage(prefixFilter, attributes) =>
          pusher.channels(prefixFilter, attributes)
        case UsersMessage(channel) =>
          pusher.users(channel)
        case AuthenticateMessage(channel, socketId, data) =>
          Future { pusher.authenticate(channel, socketId, data) }
        case ValidateSignatureMessage(key, signature, body) =>
          Future { pusher.validateSignature(key, signature, body) }
        case message =>
          throw new RuntimeException(s"Unknown message: $message")
      }
    } catch {
      case e: Exception => sender ! Status.Failure(e); throw e
    }
    future.map(new ResponseMessage(_)) pipeTo sender
  }

  override def postStop(): Unit = {
    super.postStop()
    pusher.shutdown()
  }
}

object PusherActor {
  def props(): Props = Props(new PusherActor())
}
