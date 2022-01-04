package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps



object TimersAndSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])
  system.log.info("Scheduling reminder for simpleActor")

  //implicit val executionContext = system.dispatcher
  /* Passing executionCOntext explicitly
  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "Reminder"
  }(system.dispatcher)*/
  // Implicit way of passing just import
/*
  import system.dispatcher
  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "Reminder"
  }

  val routine:Cancellable = system.scheduler.schedule(1 second,2 seconds){
    simpleActor ! "HeartBeat"
  }

  system.scheduler.scheduleOnce(5 seconds){
    routine.cancel()
  }
*/


  /* Implement  a self closing actor
  *   if the actor receives a message (anything ) ,you have    seond to send it another message
  *   - if the time window expirers the actor will stop itself
  *   if you send another message the time window is reset
  * */
  import  system.dispatcher
  class SelfClosingActor extends Actor with ActorLogging {
    var sechudle = createTimeoutWindow

    def createTimeoutWindow:Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "Timeout"
      }
    }

    override def receive: Receive = {
      case "Timeout" => log.info("Stopping my self")
          context.stop(self)
      case message => log.info(s"Recieved $message, staying alive")
          sechudle.cancel()
          sechudle = createTimeoutWindow

    }
  }
//  val selfClosingActor = system.actorOf(Props[SelfClosingActor])
//  system.scheduler.scheduleOnce(250 millis){
//    selfClosingActor ! "Ping"
//  }
//  system.scheduler.scheduleOnce( 2 seconds){
//    system.log.info("Sending PONG")
//    selfClosingActor ! "PONG"
//  }

  /* TIMERS
  *
  * */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedActor extends Actor with ActorLogging with Timers{
    timers.startSingleTimer(TimerKey,Start,500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("BootStarpping")
        timers.startPeriodicTimer(TimerKey,Reminder,1 second)
      case Reminder =>
         log.info("I am alive")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }
  val timerBasedActor = system.actorOf(Props[TimerBasedActor],"timerActor")

  system.scheduler.scheduleOnce(5 seconds){
    timerBasedActor ! Stop
  }
}
