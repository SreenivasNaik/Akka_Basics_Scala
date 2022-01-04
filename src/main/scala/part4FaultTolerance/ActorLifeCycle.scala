package part4FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {

  case object StartChild
  class LifeCycleActor extends Actor with ActorLogging{
    override def preStart(): Unit = {
      log.info("I am starting ")
    }

    override def postStop(): Unit = {
      log.info("I have Stopped")
    }
    override def receive: Receive = {
      case StartChild =>
         context.actorOf(Props[LifeCycleActor],"child")
    }
  }
  val system = ActorSystem("LifeCycleDemo")
  val parent = system.actorOf(Props[LifeCycleActor],"parent")
//  parent ! StartChild
//  parent ! PoisonPill
  object FailChild
  object Check
  object CheckChild
  class Parent extends Actor with ActorLogging {
    val child = context.actorOf(Props[Child],"supervisedCHild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }
  object  Fail
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = {
      log.info("Supervised child started")
    }

    override def postStop(): Unit = log.info("supervised Child stoped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised  child restarting because ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = log.info("supervised actor restarted")
    override def receive: Receive = {
      case Fail => log.warning("Child is Fail now")
        throw new RuntimeException("I Failed")
      case Check => log.info("I am alive")
    }
  }

  val supervisor = system.actorOf(Props[Parent],"supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild
}
