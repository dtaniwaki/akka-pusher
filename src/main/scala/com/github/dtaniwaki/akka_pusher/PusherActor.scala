package com.github.dtaniwaki.akka_pusher

import akka.actor._
import akka.pattern.pipe
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.config.{ Config, ConfigFactory }
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import net.ceedubs.ficus.Ficus._

class PusherActor(config: Config = ConfigFactory.load()) extends Actor {
  implicit val system = context.system
  implicit val ec: ExecutionContext = system.dispatcher
  private lazy val logger = LoggerFactory.getLogger(getClass)

  val pusher = new PusherClient()

  override def receive: Receive = PartialFunction { message =>
    val res = message match {
      case TriggerMessage(channel, event, message, socketId) =>
        pusher.trigger(channel, event, message, socketId)
      case ChannelMessage(channel, attributes) =>
        pusher.channel(channel, attributes)
      case ChannelsMessage(prefixFilter, attributes) =>
        pusher.channels(prefixFilter, attributes)
      case UsersMessage(channel) =>
        pusher.users(channel)
      case AuthenticateMessage(channel, socketId, data) =>
        pusher.authenticate(channel, socketId, data)
      case ValidateSignatureMessage(key, signature, body) =>
        pusher.validateSignature(key, signature, body)
      case _ =>
    }
    if (!sender.eq(system.deadLetters) && !sender.eq(ActorRef.noSender)) {
      res match {
        case future: Future[_] =>
          future pipeTo sender
        case res if !res.isInstanceOf[Unit] =>
          sender ! res
        case _ =>
      }
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    pusher.shutdown()
  }
}

object PusherActor {
  def props(): Props = Props(new PusherActor())
}
