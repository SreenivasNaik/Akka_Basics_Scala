package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2Actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  object FussyKid{
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._
    var state  = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }
  object Mom{
    case class MomStart(kidRef:ActorRef)
    case class Food(food: String)
    case class Ask(message:String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
          kidRef ! Food(VEGETABLE)
          kidRef ! Ask("Do you want to play")
      case KidAccept => println("yaY He is happy")
      case KidReject => println("yaY He is not happy but Healthy")
    }

  }

  val system = ActorSystem("ChangingBehavior")
  val fussyKid = system.actorOf(Props[FussyKid],"fussyKid")
  val mom = system.actorOf(Props[Mom],"Mom")

 // mom ! MomStart(fussyKid)


  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive
    def happyReceive:Receive = {
      case Food(VEGETABLE) => context.become(sadReceive,false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive:Receive = {
      case Food(VEGETABLE) =>  context.become(sadReceive,false)// Stay Sad
      case Food(CHOCOLATE) => context.unbecome()// change my recieve handler to sad
      case Ask(_) => sender() ! KidAccept
    }
  }
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid],"statelessFussyKid")
  mom ! MomStart(statelessFussyKid)

  /* Mom receives MoMStart

  * FOod(Veg) => stack.push(sadReceive)
    Food(Choc) => stack.push(HappyRecive)

    Stack
    1. happyRecieve
    2. sadRecieve
  * */

  /* Excersizes
      1 . Counter actor with context become
       2, SIMPlified Voting system

  *
  * */
  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }
  class CounterActor extends Actor{
    import CounterActor._
    var count =0

    override def receive: Receive = counterReceive(0)
    def counterReceive(currentCount: Int):Receive = {
      case Increment => println(s"[$currentCount] Incrementing")
        context.become(counterReceive(currentCount+1))
      case Decrement => println(s"[$currentCount]Decrementing")
        context.become(counterReceive(currentCount - 1))
      case Print => println("[Counter] My currentCounter "+currentCount)
    }
  }

  val counterActor = system.actorOf(Props[CounterActor],"counterActor")
  (1 to 5).foreach(_=> counterActor ! CounterActor.Increment)
  (1 to 3).foreach(_=> counterActor ! CounterActor.Decrement)
  counterActor ! CounterActor.Print

  /* Excersies -2  a simplified Voting system

  * */
  case object VoteStatusRequest
  case class Vote(candidate:String)
  case class VoteStatusReply(candidate:Option[String])
    class Citizen extends Actor{
      var candidate:Option[String] = None
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))//candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }
      def voted(candidate: String):Receive={
        case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
      }
  }
  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor{
//    var stillWaiting:Set[ActorRef] = Set()
//    var currentStatus:Map[String,Int] = Map()
    override def receive: Receive = awaitingCommand

    def awaitingCommand:Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizens => citizens ! VoteStatusRequest)
        context.become(awaitingStatus(citizens,Map()))
    }
    def awaitingStatus(stillWaiting: Set[ActorRef], currentStatus: Map[String, Int]):Receive = {
      case VoteStatusReply(None) => // citizen not voted
        sender() ! VoteStatusRequest
      case VoteStatusReply(Some(value)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotes = currentStatus.getOrElse(value,0)
        val newStats = currentStatus + (value ->(currentVotes+1))
        if(newStillWaiting.isEmpty){
          println(s"[Aggregator] poll status :: $newStats")
        }else{
            context.become(awaitingStatus(newStillWaiting,newStats))
        }
    }
  }

  val a = system.actorOf(Props[Citizen])
  val b = system.actorOf(Props[Citizen])
  val c = system.actorOf(Props[Citizen])
  val d = system.actorOf(Props[Citizen])

  a ! Vote("Martin")
  b ! Vote("Jonas")
  c ! Vote("Roland")
  d ! Vote("Roland")
  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(a,b,c,d))

  // Print status of votes -> Martin - 1 Jonas -1 Roland - 2

}
