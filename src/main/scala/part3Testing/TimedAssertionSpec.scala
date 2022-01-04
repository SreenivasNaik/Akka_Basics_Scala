package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionSpec extends TestKit(ActorSystem("Timed",ConfigFactory.load("mySpecialConfig")))
with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import TimedAssertionSpec._
  "A workers  Actor " should{
    val workerActor = system.actorOf(Props[WorkerActor])
    "Replly with in timely manner" in {
      within(500 millis, 1 second) {
          workerActor ! "work"
          expectMsg(WorkResult(42))
      }
    }
    "reply with valid work at " in {
      within(1 second) {
        workerActor ! "workSequence"
        val results: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10) {
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
      }
    }
    "reply to test prob" in {
        within(1 second) {
          val prob = TestProbe()
          prob.send(workerActor, "work")
          prob.expectMsg(WorkResult(42))
        }

      }

  }

}
object TimedAssertionSpec{
  case class WorkResult(result:Int)
  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // Long Computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case  "workSequence" =>
        val r = new Random()
        for ( i <- 1 to 10){
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}
