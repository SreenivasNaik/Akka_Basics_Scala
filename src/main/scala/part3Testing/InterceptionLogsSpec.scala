package part3Testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptionLogsSpec  extends TestKit(ActorSystem("InterceptingLogs",ConfigFactory.load().getConfig("interceptingLogMessages")))
with ImplicitSender with AnyWordSpecLike
  with BeforeAndAfterAll
{
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import InterceptionLogsSpec._
  val item = "Rock The JVM"
  val creditCard = "1234-1234"
  "A checkout flow " should{
    "correctly log the dispatch Order " in {
      EventFilter.info(pattern = s"Order [0-9]+ for Item $item has been dispatched",occurrences = 1) intercept{
          val checkoutRef = system.actorOf(Props[CheckoutActor])
          checkoutRef ! Checkout(item,creditCard)
      }
    }
  }

  "Freak out If the payment denied " in {
    EventFilter[RuntimeException](occurrences = 1) intercept {
      val checkoutRef = system.actorOf(Props[CheckoutActor])
      checkoutRef ! Checkout(item,"000-00")
    }
  }

}

object InterceptionLogsSpec{
  case class Checkout(item:String,creditCard:String)
  case class AuthorizeCard(card: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed
  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfilmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingForCheckout
    def awaitingForCheckout:Receive = {
      case Checkout(item,card) =>
        paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }
    def pendingPayment(item: String):Receive = {
      case PaymentAccepted =>
          fulfilmentManager ! DispatchOrder(item)
        context.become(pendingFulfilment(item))
      case PaymentDenied => throw new RuntimeException("I can;t handle")
    }
    def pendingFulfilment(item: String):Receive = {
      case OrderConfirmed => context.become(awaitingForCheckout)
    }
  }
  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
          if (card.startsWith("0")) sender() ! PaymentDenied
          else sender() ! PaymentAccepted
    }


  }
  class FulfillmentManager extends Actor with ActorLogging{
      var orderId = 43
    override def receive: Receive = {
      case DispatchOrder(item:String) => {
        orderId += 1
        log.info(s"Order $orderId for Item $item has been dispatched")
         sender() ! OrderConfirmed
      }
    }
  }
}
