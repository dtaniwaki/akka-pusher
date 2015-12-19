package com.github.dtaniwaki.akka_pusher

import akka.actor._
import akka.pattern.pipe
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.config.{ Config, ConfigFactory }
import scala.collection.mutable.Queue
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import net.ceedubs.ficus.Ficus._

import scala.util.{ Success, Failure }

class PusherActor(
    config: Config,
    private val batchTriggerQueue: Queue[TriggerMessage]) extends Actor with PusherJsonSupport {

  // NOTE: Define redundant constructor to avoid an error to create prop without arguments.
  def this() = {
    this(config = ConfigFactory.load(), batchTriggerQueue = Queue[TriggerMessage]())
  }

  implicit val system = context.system
  implicit val ec: ExecutionContext = system.dispatcher
  private lazy val logger = LoggerFactory.getLogger(getClass)

  val batchNumber = 100
  val batchTrigger = config.as[Option[Boolean]]("pusher.batchTrigger").getOrElse(false)
  val batchInterval = Duration(config.as[Option[Int]]("pusher.batchInterval").getOrElse(1000), MILLISECONDS)
  protected val scheduler = if (batchTrigger) {
    Some(system.scheduler.schedule(
      batchInterval,
      batchInterval,
      self,
      BatchTriggerTick()))
  } else {
    None
  }

  logger.debug("PusherActor configuration:")
  logger.debug(s"batchTrigger........ ${batchTrigger}")
  logger.debug(s"batchInterval....... ${batchInterval}")

  val pusher = new PusherClient()

  override def receive: Receive = PartialFunction { message =>
    val res = message match {
      case trigger: TriggerMessage if batchTrigger =>
        batchTriggerQueue.enqueue(trigger)
        true
      case triggers: Seq[_] if batchTrigger && triggers.forall(_.isInstanceOf[TriggerMessage]) =>
        batchTriggerQueue.enqueue(triggers.map(_.asInstanceOf[TriggerMessage]): _*)
        true
      case TriggerMessage(channel, event, message, socketId) =>
        pusher.trigger(channel, event, message, socketId)
      case triggers: Seq[_] if triggers.forall(_.isInstanceOf[TriggerMessage]) =>
        Future.sequence(triggers.map(_.asInstanceOf[TriggerMessage]).grouped(batchNumber).map { triggers =>
          pusher.trigger(triggers.map(TriggerMessage.unapply(_).get))
        })
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
      case trigger: BatchTriggerMessage if batchTrigger =>
        batchTriggerQueue.enqueue(TriggerMessage.tupled(BatchTriggerMessage.unapply(trigger).get))
        true
      case BatchTriggerTick() if batchTrigger =>
        val triggers = batchTriggerQueue.dequeueAll { _ => true }
        Future.successful(triggers).map { triggers =>
          triggers.grouped(batchNumber) foreach { triggers =>
            pusher.trigger(triggers.map(TriggerMessage.unapply(_).get)).map {
              case Success(_) => // Do Nothing
              case Failure(e) => logger.warn(e.getMessage)
            }
          }
        }
        triggers.length
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
    if (batchTrigger) {
      scheduler.map(_.cancel())
    }
    pusher.shutdown()
    super.postStop()
  }
}

object PusherActor {
  def props(): Props = Props(classOf[PusherActor])
  def props(config: Config, batchTriggerQueue: Queue[TriggerMessage]): Props = Props(classOf[PusherActor], config, batchTriggerQueue)
}
