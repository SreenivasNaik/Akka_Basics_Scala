package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import javafx.print.Printer
import part2Actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App {

  class SimpleActor extends Actor{
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello There" // Replying
      case message:String => println(s"${context.self} I have received $message")
      case message:Int => println(s"${self} I have received number $message")
      case SpecialMessage(content) => println(s"[Simple Actor] I have received SpecialMessage $content")
      case SendMessageToYourSelf(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhone(conent,ref) => ref forward conent
    }
  }
  val system = ActorSystem("actorSystemDemo")
  val simpleActor = system.actorOf(Props[SimpleActor],"simpleActor")

  simpleActor ! "Hello Actor"
  simpleActor ! 23

  case class SpecialMessage(content:String)
  simpleActor ! SpecialMessage("Hello")
/*
*   1. MESSAGES can be any type
*   2. Must be IMMUTABLE
*   3. Must be SERIALIZABLE
*
* USE CASE CLASSES AND CASE OBJECTS
* */

  /*  2. Actors have info about themslef and context
  *   context.self === this in OOPS
  * */

  case class SendMessageToYourSelf(content:String)
  simpleActor ! SendMessageToYourSelf("send")

 /* 3 . Actors can REPLY to messages
 *
 * */

  val sree= system.actorOf(Props[SimpleActor],"sree")
  val naik= system.actorOf(Props[SimpleActor],"naik")
  val cnu= system.actorOf(Props[SimpleActor],"cnu")

  case class SayHiTo(ref:ActorRef)
  sree ! SayHiTo(naik)

  // 4. Dead Letters
  sree ! "Hi!"

  // 5 . Forwarding Messages D -> A -> B Forwarding ==> sending messages with the original sender

  case class WirelessPhone(conent:String,ref:ActorRef)
  sree ! WirelessPhone("Hi",naik)

  /*  Excercises
  *   1 . Counter Actor
  *     -- increment
  *      -- decrement
  *     -- print
  *
  * 2. Bank acount as Actor
  *     ->Deposit
  *     -> WithDrwa
  *      -> Statement
  *   replies with sucess or failure
  *   interact with some other kind of actor
  * */


    // DOMAIN of the counter
  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }
  class CounterActor extends Actor{
    import CounterActor._
    var count =0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[Counter] I have the count $count")
    }
  }

  val counterActor = system.actorOf(Props[CounterActor],"counterActor")
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Decrement
  counterActor ! CounterActor.Print


  // Bank Acount
  object BankAccount {
    case class Deposit(amount:Int)
    case class WithDraw(amount:Int)
    case object Statement
    case class TransactionSucess(message:String)
    case class TransactionFailure(message:String)
  }
  class BankAccount extends Actor{
    import BankAccount._
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>{
        if (amount < 0) sender() ! TransactionFailure("Invalid amount")
        else {
          funds += amount
          sender() ! TransactionSucess(s"Sucessfully deposited $amount")
        }
      }
      case WithDraw(amount) => {
        if (amount > funds) sender() ! TransactionFailure("Invalid withdraw amount")
        else {
          funds -= amount
          sender() ! TransactionSucess(s"Sucessfully withdraw $amount")
        }
      }
      case Statement =>  sender() ! s"The balance funds $funds"
    }
  }

  object Person{
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor{
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) => {
        account ! Deposit(10000)
        account ! WithDraw(100000)
        account ! WithDraw(4000)
        account ! Statement
      }
      case message => println(message.toString)

    }
  }

  val account = system.actorOf(Props[BankAccount],"bankAcount")
  val person = system.actorOf(Props[Person],"person")
  person ! LiveTheLife(account)
}
