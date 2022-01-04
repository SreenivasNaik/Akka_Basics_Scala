package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging{

    var count =0

    override def receive: Receive = {
      case message =>
        count+=1
        log.info(s"[$count] ${message.toString}")

    }
  }
  val system = ActorSystem("DispatcherDemo")//,ConfigFactory.load().getConfig("dispatcherDemo")

  // method :1 Programmtic way
//  val simpleCounterActor = system.actorOf(Props[Counter].withDispatcher("my-dispatcher"))
  val Actors = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"),s"Counter_$i")
  val r = new Random()
//  for(i<- 1 to 1000){
//    Actors(r.nextInt(10)) ! i
//  }

  // From COnfig
  val sreenu = system.actorOf(Props[Counter],"sreenu")

  /*
  *  Dispatchers implement the ExecutionContext trait
  * */
  class DBActor extends Actor with ActorLogging {
    //implicit val executionContext:ExecutionContext = context.dispatcher
    // soultion -1
    implicit val executionContext:ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    // Soution 2 -> routers
    override def receive: Receive = {
      case message => Future {
        // wait on resource
        Thread.sleep(5000)
        log.info(s"Success $message")
    }}
  }
  val dBActor = system.actorOf(Props[DBActor],"DBActor")

  //dBActor ! "Hello "
  val nonBlockingActor = system.actorOf(Props[Counter],"NonBlockingActor")
  for(i<- 1 to 1000) {
    val message = s"Important message $i"
    dBActor ! message
    nonBlockingActor ! message
  }
}
