package part4FaultTolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("Supervision"))
 with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._
  "A Supervisor" should{
    "Resume child incase of minor Fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "I love Akka"
      child ! Report
      expectMsg(3)
      child ! "Il ove akka kakka hsshsjssdhjsdjsd shjsdhjs hjsdhjsd hsdjksd"
      child ! Report
      expectMsg(3)
    }
    "Restart the child in case an empty sentance" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "I love Akka"
      child ! Report
      expectMsg(3)
      child ! ""
      child ! Report
      expectMsg(0)
    }
    "Terminate the child in case an Major Error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)
      child ! "sreenu"
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }
    "Escalate An error whe it doesn't know what to do " in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)
      child ! 34
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)

    }
  }
  "A kind Supervisor" should {
    "not kill childerns" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestart],"NoDeathSUper")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
    child ! "Akka cool"
      child ! Report
      expectMsg(2)
      child ! 42
      child ! Report
      expectMsg(0)
    }
  }
  "All FOr One supervisor" should {
    "Apply all for one " in {
      val supervisor = system.actorOf(Props[AllFOrOne],"AllForOneSup")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]
      secondChild ! "Akka cool"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child ! ""
      }
      Thread.sleep(500)
      secondChild ! Report
      expectMsg(0)
    }
  }
}
object SupervisionSpec{
  case object Report
  class Supervisor extends Actor {
    override val supervisorStrategy:SupervisorStrategy = OneForOneStrategy(){
      case _:NullPointerException => Restart
      case _:IllegalArgumentException => Stop
      case _:RuntimeException => Resume
      case _:Exception => Escalate
    }
    override def receive: Receive = {
      case props:Props =>
        val child = context.actorOf(props)
        sender() ! child
    }
  }
  class NoDeathOnRestart extends Supervisor{
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

    }
  }
  class AllFOrOne extends Supervisor{
    override val supervisorStrategy = AllForOneStrategy(){
      case _:NullPointerException => Restart
      case _:IllegalArgumentException => Stop
      case _:RuntimeException => Resume
      case _:Exception => Escalate
    }
  }
  class FussyWordCounter extends Actor {
    var words = 0
    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentance is empty")
      case sentance:String =>
        if( sentance.length > 20 ) throw new RuntimeException("Sentance is too big")
        else if ( !Character.isUpperCase(sentance(0))) throw new IllegalArgumentException("Sentance should start with Uppercase")
        else words += sentance.split(" ").length
      case _ => throw new Exception("Can only recieve strings")
    }
  }
}
