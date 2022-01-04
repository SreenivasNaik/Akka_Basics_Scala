package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3Testing.BasicSpec.{Blackhole, LabTestActor, SimpleActor}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  "A Simple Actor " should {
    "send back the same message" in {
      // testing scenario
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "Hello"
      echoActor ! message
      expectMsg(message)
    }
  }
  "A Blockhole Actor " should{
    "send back the same message" in {
      // testing scenario
      val blackhole = system.actorOf(Props[Blackhole])
      val message = "Hello"
      blackhole ! message
      expectNoMessage(1 second)
    }
  }

  "A lab Test actor " should{
    val labTestActor = system.actorOf(Props[LabTestActor])
    "turn a string into uppercase " in {
        labTestActor ! "I love akka"
      val reply = expectMsgType[String]
      assert(reply == "I LOVE AKKA")
      //expectMsg("I LOVE AKKA")
    }
    "reply to greet " in {
      labTestActor ! "greet"
      expectMsgAnyOf("Hi","Hello")
      //expectMsg("I LOVE AKKA")
    }
  }

}
object BasicSpec{
  class SimpleActor extends Actor{
    override def receive: Receive =
      {
        case message => sender() ! message
      }

  }
  class Blackhole extends Actor{
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor{
    val random = new Random()
    override def receive: Receive = {
      case "greet" => if(random.nextBoolean()) sender() ! "Hi" else sender() ! "Hello"
      case message:String => sender() ! message.toUpperCase
    }
  }
}
