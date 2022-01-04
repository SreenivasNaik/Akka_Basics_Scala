package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object MailBoxes extends App {

  val system = ActorSystem("MailBoxDemo",ConfigFactory.load().getConfig("mailBoxDemo"))
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /*
  *   Intereseting Case 1 => custom Priority mailboxes
  *   PO => Most Important
  *   P1,P2....
  * */

  // Step:1  Mail Box defination
  class SupportTicketPriorityMailBox(settings:ActorSystem.Settings,config:Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator{
      case message:String if message.startsWith("[P0]") => 0
      case message:String if message.startsWith("[P1]") => 1
      case message:String if message.startsWith("[P2]") => 2
      case message:String if message.startsWith("[P3]") => 3
      case _ => 4
    }
  )
   // Step 2 => make it known in the config

    // Step 3 => attch the dispatcher to an actor
    val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("supportTicketDispatcher"))
//    supportTicketActor ! PoisonPill
//    supportTicketActor ! "[P3] THIS CAN BE SLOVE LATER "
//    supportTicketActor ! "[P1] THIS CAN BE SLOVE BIT FASTER "
//    supportTicketActor ! "[P0] THIS SHOOULD SLOVE NOW"
//    supportTicketActor ! "[P2] THIS CAN BE WAIT "

   // After which time can i send another message and be priortized accordingly

   /*
   *  Interesting Case #2 => Control - Aware - mail box
   *  will use UnboundedControlAwareMailBox from the Akka
   * */
// Step 1 => mark important message as controled message
  case object ManagementTicket extends ControlMessage

  /* step-2 Configure who gets the mailbox - make actor attach to the mailbox
  * */
  // Method -1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withDispatcher("control-mailbox"))

//  controlAwareActor ! "[P0] THIS SHOOULD SLOVE NOW"
//  controlAwareActor ! "[P2] THIS CAN BE WAIT "
//  controlAwareActor ! ManagementTicket

  // Method 2 => using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor],"altControlAwareActor")

  altControlAwareActor ! "[P0] THIS SHOOULD SLOVE NOW"
  altControlAwareActor ! "[P2] THIS CAN BE WAIT "
  altControlAwareActor ! ManagementTicket
}
