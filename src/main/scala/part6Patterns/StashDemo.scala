package part6Patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /*  Resource Actor =>
      if it open => it can receive read/write requests to the resources
       -> otherwise => it will postpone all the read/ write requests until the state is chnages to open
  * when Resource actors start it will be in closed state
        => OPEN => switch to open state
          => READ,WRITE => will be POSTPONED
    => when it receive OPen => it will change to OPen state
            => Read ,write
            => Close t=> switch to closed state

            [OPEN,READ,READ,WRITE] =>
              switch to open state
                Read the data
                Read the data
                Write the data
           [READ,OPEN,WRITE]
           -> stash read Stash[Read]
           -> open => switch to open
           MailBOX => [Read,Write]
           handle them

  * */

  case object Open
  case object Close
  case object Read
  case class Write(data:String)

  // Step -1 MIx in the Stash Trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData :String = ""
    override def receive: Receive = closed

    def closed:Receive = {
      case Open => log.info("Opening the resource")
        // step 3 : unstashAll when we switch the message handler
        unstashAll()
        context.become(open)
      case message => log.info(s"I can;t handle [$message] in closed state ")
        // Step -2 Stash away what mssages you want
        stash()
    }
    def open:Receive = {
      case Read =>
          log.info(s"I have read [$innerData]")
      case Write(data) => log.info(s"I am Writing $data")
        innerData = data
      case Close =>
        log.info("I am closing")
        unstashAll()
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"I can;t handle [$message] in closed state ")
        // Step -2 Stash away what mssages you want
        stash()
    }

  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor],"ResourceActor")
  resourceActor ! Write("I love you")
  resourceActor ! Read
  resourceActor ! Open
}
