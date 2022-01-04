package part2Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLogging extends App {

  // 1 . Explicit Logging
  class SimpleActorWithExplicitLogger extends Actor{
    val logger = Logging(context.system,this)
    /* 1 -DEBUG
      2 - INFO
      3 - WARNING/ WARN
      4 - ERROR
    *
    * */
    override def receive: Receive = {
      case message => logger.info(message.toString)
    }

  }
  val system = ActorSystem("LoggingDemo")
  val simpleActorWithExplicitLogger = system.actorOf(Props[SimpleActorWithExplicitLogger],"sawel")
  simpleActorWithExplicitLogger ! " Logging simple message"

  // 2 Actor Logging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a,b) => log.info("two thinss: {} and {}",a,b)
      case msg => log.info(msg.toString)
    }
  }
  val actorWithLogging = system.actorOf(Props[ActorWithLogging],"actorwiht")
  actorWithLogging ! "Logging simple mssage"
  actorWithLogging ! (32,3)
}
