package part4FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingAndStoppingActors  extends App {

  val system = ActorSystem("stopingActorDemo")
  object Parent{
    case class StartChild(name:String)
    case class StopChild(name:String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildern(Map())
    def withChildern(children:Map[String,ActorRef]):Receive = {
      case StartChild(name) =>
        log.info(s"Starting Child $name")
        context.become(withChildern(children+(name -> context.actorOf(Props[Child],name))))
      case StopChild(name) =>
        log.info(s"Stoping Child with the name : $name")
        val childOption  = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
         log.info("Stoping My self")
        context.stop(self)
    }
  }
  class Child extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  /*
  *  Method - 1 using context.stop(actorRef)
  * */
  import Parent._
//  val parent = system.actorOf(Props[Parent],"parent")
//  parent ! StartChild("child-1")
//  val child = system.actorSelection("/user/parent/child-1")
//  child ! "Hello Child"

 // parent ! StopChild("child-1")
 // for(_ <- 1 to 50) child ! "are you still there "

//  parent ! StartChild("child-2")
//  val child2 = system.actorSelection("/user/parent/child-2")
//  child2 ! "Hello Child2"
//  parent ! Stop
//   for(i <- 1 to 50) parent ! s"[${i}]Parent are you still there "
//   for(i <- 1 to 50) child ! s"[${i}]child1 are you still there "
//   for(i <- 1 to 50) child2 ! s"[$i]child2 are you still there "

  /*
    Method -2 Using Special Messages
  * */
/*  val looseActor = system.actorOf(Props[Child])
  looseActor ! "Hello Loose"
  looseActor ! PoisonPill
  looseActor ! "are you still there"

  val terminatedActor = system.actorOf(Props[Child])
  terminatedActor ! "You are going to kill"
  terminatedActor ! Kill
  terminatedActor ! "You have beeb  killed"*/

  /*
  *  Method - 3 Death Watch
  * */

  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name ) =>
        val child = context.actorOf(Props[Child],name)
        log.info(s"Started and Watching child $child")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"The refference that i am watching $ref has been stopped")
    }
  }
  val watcher = system.actorOf(Props[Watcher],"Watcher")
  watcher ! StartChild(name = "WatchedChild")
  val watchedChild = system.actorSelection("/user/Watcher/WatchedChild")
  Thread.sleep(500)
  watchedChild ! PoisonPill

}
