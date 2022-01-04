package part6Patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest}
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import part6Patterns.FSMSpec.VendingErrorCode.{MachineNotInitialized, ProductNotAvailable, RequestTimedOut}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


class FSMSpec extends TestKit(ActorSystem("FSM")) with
ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll with OneInstancePerTest
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpec._
  def runTestSuite(props:Props) ={
    "Error when not initialized" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError(MachineNotInitialized))
    }
    "Report a product not available" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke"->10),Map("coke"->1))
      vendingMachine ! RequestProduct("car")
      expectMsg(VendingError(ProductNotAvailable))
    }
    "Throw Timeout if I don;t insert money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke"->10),Map("coke"->1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 dollors"))
      within(1.5 seconds) {
        expectMsg(VendingError(RequestTimedOut))
      }
    }
    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke"->10),Map("coke"->3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollors"))
      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))
      within(1.5 seconds){
        expectMsg(VendingError(RequestTimedOut))
        expectMsg(GiveBackChange(1))
      }
    }
    "Deliver Product money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke"->10),Map("coke"->3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollors"))
      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }
    "Giveback change and be able to request money for a new Product" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke"->10),Map("coke"->3))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollors"))
      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollors"))
    }
  }

  "A Vending machine " should{
  runTestSuite(Props[VendingMachine])
  }

  "A Vending machine FSM " should{
   runTestSuite(Props[VendingMachineFSM])
  }
}
object FSMSpec{

  /* Vending Machine
  *
  * */

  case class Initialize(inventory:Map[String,Int],prices:Map[String,Int])
  case class RequestProduct(product:String)

  case class Instruction(instruction:String) // Message the VM will show on its screen
  case class ReceiveMoney(amount:Int)
  case class Deliver(product:String)
  case class GiveBackChange(amount:Int)

  case class VendingError(reason:String)
  case object ReceiveMoneyTimeout

  object VendingErrorCode{
    val MachineNotInitialized = "MachineNotInitialized"
    val ProductNotAvailable = "ProductNotAvailable"
    val RequestTimedOut = "RequestTimedOut"
  }

  class VendingMachine extends Actor with ActorLogging{
    implicit val executionContext:ExecutionContext = context.dispatcher
    override def receive: Receive = idle

    def idle:Receive = {
      case Initialize(inventory,prices) => context.become(operational(inventory,prices))
      case _ => sender() ! VendingError(MachineNotInitialized)
    }
    def operational(inventory: Map[String, Int], prices: Map[String, Int]):Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) => sender() ! VendingError(ProductNotAvailable)
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price dollors")
          context.become(waitForMoney(inventory,prices,product,0,startReceiveMoneyTimeoutSchedule,sender()))
      }
    }
    def waitForMoney(inventory:Map[String,Int],
                     prices:Map[String,Int],
                     product:String,
                     money:Int,
                     moneyTimeOutShedule:Cancellable,
                     requester:ActorRef

                    ):Receive = {
      case ReceiveMoneyTimeout => requester ! VendingError(RequestTimedOut)
            if(money > 0 ) requester ! GiveBackChange(money)
        context.become(operational(inventory,prices))
      case ReceiveMoney(amount) =>
        moneyTimeOutShedule.cancel()
        val price = prices(product)
        if(money + amount >= price) {
          // User buys
          requester ! Deliver(product)
          if(money+amount - price >0 ) requester ! GiveBackChange(money+amount - price)
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product->newStock)
          context.become(operational(newInventory,prices))
        }
        else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          context.become(waitForMoney(inventory,prices,product,
            money+amount, // User has inserted some money
            startReceiveMoneyTimeoutSchedule, // we have to setup the timeout again
            requester))
        }
    }
    def startReceiveMoneyTimeoutSchedule = context.system.scheduler.scheduleOnce(1 second){
      self ! ReceiveMoneyTimeout
    }
  }

  // STEP -1 DEFINE the states and the data of the actor
    trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(inventory:Map[String,Int],
                              prices:Map[String,Int],
                              product:String,
                              money:Int,
                              requester:ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState,VendingData] {
    // We don;t have a receive handler
    // WHen FSM receives the message it will trigger the EVENT(message,data)
    /* State , Data
    * Event => state and data can be change
    *
    * state => idle data = Uninitialized
    * Event(Initialize(Map("coke"->10),Map(coke->1))
    *   => State : Operational
    *       Data : Initialized(Map("coke"->10),Map(coke->1))
    *
    *  Event(RequestProduct(coke)) =>
    *
    * */
    startWith(Idle,Uninitialized)
    when(Idle){
      case Event(Initialize(inventory,prices),Uninitialized) =>
        goto(Operational) using Initialized(inventory,prices)
      // This is equivalent with context.become(operational(inventory,prices))
      case _ =>
        sender() ! VendingError(MachineNotInitialized)
        stay()
    }
    when(Operational){
      case Event(RequestProduct(product),Initialized(inventory,prices)) => inventory.get(product) match {
        case None | Some(0) => sender() ! VendingError(ProductNotAvailable)
          stay()
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price dollors")
          goto(WaitForMoney) using WaitForMoneyData(inventory,prices,product,0,sender())
      }
    }
    when(WaitForMoney,stateTimeout = 1 seconds){
      case Event(StateTimeout,WaitForMoneyData(inventory,prices,product,money,requester)) =>
        requester ! VendingError(RequestTimedOut)
        if(money > 0 ) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory,prices)
      case Event(ReceiveMoney(amount),WaitForMoneyData(inventory,prices,product,money,requester) )=> {
        val price = prices(product)
        if(money + amount >= price) {
          // User buys
          requester ! Deliver(product)
          if(money+amount - price >0 ) requester ! GiveBackChange(money+amount - price)
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product->newStock)
          goto(Operational) using Initialized(newInventory,prices)
        }
        else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert $remainingMoney dollars")
          stay() using WaitForMoneyData(inventory,prices,product,money+amount,requester)
        }
      }
    }
    whenUnhandled{
      case Event(_,_) => sender() ! VendingError("CommandNotFound")
        stay()

    }
    onTransition{
      case stateA -> stateB => log.info(s"Transitioning From $stateA to $stateB")
    }

    initialize()


  }

}
