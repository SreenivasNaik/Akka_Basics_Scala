package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /*
  * 1 . Manual Routers
  * */
  class Master extends Actor with ActorLogging{
    // step:1 Create Actor Routees
    // 5 actor routees based off slave actors
    private val slaves = for(i<- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave],s"Slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    // Step:2 Define the router
    private var router = Router(RoundRobinRoutingLogic(),slaves)
    override def receive: Receive ={
      // Step:3 Route the messages

      case message =>
        router.route(message,sender())
      // Step:4 handle the termination/ lifecycle of the routees
      case Terminated(ref) =>
         router = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router = router.addRoutee(newSlave)
    }
  }

  class Slave extends Actor with ActorLogging{

    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RouterDemo",ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])
/*
  for(i <- 1 to 10) {
    master ! s"Hello World -->[${i}]"
  }
*/

  /* Method # 2 => a router actor with its own childerns
        POOL router
  * */
  // 2.1 Programmatic
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]),"simplePoolMaster")
//  for(i <- 1 to 10) {
//    poolMaster ! s"Hello World -->[${i}]"
//  }
  // 2.2 From THE configuration

  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]),"poolMaster2")
 /* for(i <- 1 to 10) {
    poolMaster2 ! s"Hello World -->[${i}]"
  }*/


  /* Method 3 => Router with actors created elsewhere
  *     GROUP Router
  *  */

  val slaveList = (1 to 5).map(i=> system.actorOf(Props[Slave],s"slave_$i")).toList
  // need actor paths
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  // 3.1 in the code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
//  for(i <- 1 to 10) {
//    groupMaster ! s"Hello World -->[${i}]"
//  }

  // 3.2 From COnfiguration

  val groupMaster2 = system.actorOf(FromConfig.props(),"groupMaster2")
//  for(i <- 1 to 10) {
//    groupMaster ! s"Hello World -->[${i}]"
//  }

  /*
  *  SPECIAL MESSAGES
  * */

  groupMaster2 ! Broadcast("hello")

  groupMaster2 ! PoisonPill
}
