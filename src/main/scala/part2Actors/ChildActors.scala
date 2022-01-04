package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.sun.tools.javac.comp.Check
import part2Actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}
import part2Actors.ChildActors.ParentActor.{CreateChild, TellChild}

object ChildActors extends App {

  object ParentActor{
    case class CreateChild(name:String)
    case class TellChild(message:String)
  }
   class ParentActor extends Actor{
    import ParentActor._

     override def receive: Receive = {
       case CreateChild(name) =>
          println(s"${self.path} creating child actor")
          // Create a child actor
         val childRef = context.actorOf(Props[ChildActor],name)
         context.become(withChild(childRef))

     }
     def withChild(ref: ActorRef):Receive ={
       case TellChild(message) =>
         if(ref != null ) ref forward  message
     }
   }
  class ChildActor extends Actor{
    override def receive: Receive = {
      case message => println(s"[${self.path}] Received a message ${message.toString}")
    }
  }
val system = ActorSystem("ParentChild")
  val parent = system.actorOf(Props[ParentActor],"parent")
  parent ! CreateChild("ChildActor")
  parent ! TellChild("hello Child")

  /* Actor Hierarchies
  *   Parent -> Child
  *
  *   /System ==> System levl
  *   /user ==> user level ==> system.actorOf
  *   / ==> root level
  * */

  /*
  *  Actor Selection
  * */

  val childActor = system.actorSelection("/user/parent/ChildActor")
  childActor ! "I found you"

  /* Danger
  *  NEVER PASS MUTABLE ACTOR STATE OR THE 'THIS` REFERENCE TO CHILD ACTORS
  *  NEVER
  * */

  object NaiveBankAccount{
    case class Deposite(amount:Int)
    case class Withdraw(amount:Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor{
    import NaiveBankAccount._
    import CreditCard._
    var amount =0
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef =context.actorOf(Props[CreditCard],"credit")
        creditCardRef ! AttachToAccount(self)
      case Deposite(funds ) => deposite(funds)
      case Withdraw(funds ) => withDraw(funds)
    }
    def deposite(funds:Int) ={
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds}
    def withDraw(funds:Int) = {
      println(s"${self.path} withdrawing $funds on top of $amount")
      amount -= funds
    }
  }

  object CreditCard{
    case class AttachToAccount(bankAccountRef: ActorRef) // ???
    case object CheckStatus
  }
  class CreditCard extends Actor{
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachToAccount(account))
    }
    def attachToAccount(account: ActorRef):Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed ")
        // begin
        //account.withDraw(1)

    }
  }
  import CreditCard._
  import NaiveBankAccount._
  val bankAccount = system.actorOf(Props[NaiveBankAccount],"account")
  bankAccount ! InitializeAccount
  bankAccount ! Deposite(1000)

  Thread.sleep(500)
  val ccSelection = system.actorSelection("/user/account/credit")
  ccSelection ! CheckStatus
}
