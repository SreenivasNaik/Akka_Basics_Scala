package part3Testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbSpec  extends TestKit(ActorSystem("TestProbSpec"))
with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import TestProbSpec._

  "A master Actor " should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }
    "send the work to slave actor " in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val work = " I love akka"
      master ! Work(work)

      slave.expectMsg(SlaveWork(work,testActor))
    }
  }

}
object TestProbSpec{
  // scenario
  /*
  * Word counting actor hierarchey with master - slave
  *  send some work to master
  *   master send the slave the piece of work
  *   slave processes work and replies to master
  *   master aggregates the result
  * master send the total count to the original requester
  *
  * */
  case class Register(slaveRef:ActorRef)
  case class Work(text:String)
  case class SlaveWork(text:String,originalReq:ActorRef)
  case class WorkCompleted(count:Int,originalReg:ActorRef)
  case class Report(count: Int)
  case class RegistrationAck()
  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegistrationAck
        context.become(online(slaveRef,0))
    }
    def online(salveRef: ActorRef, totalwordCount:Int):Receive = {
      case Work(text) => salveRef ! SlaveWork(text,sender())
      case WorkCompleted(count,originalReg) =>
        val newTotal = totalwordCount+count
        originalReg ! Report(newTotal)
        context.become(online(salveRef,newTotal))
    }
  }
}
