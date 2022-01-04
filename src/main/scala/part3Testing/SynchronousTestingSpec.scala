package part3Testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll

import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll{
  implicit val system = ActorSystem("SynchronousTest")

  override def afterAll(): Unit = {
    system.terminate()
  }
  import SynchronousTestingSpec._
  "A Counter" should {
    "Synchronously increase it's counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc // counter has already recived message

      assert(counter.underlyingActor.count == 1)
    }
      "Sycnronoys increase its counter" in {
        val counter = TestActorRef[Counter](Props[Counter])
        counter.receive( Inc) // counter has already recived message
        assert(counter.underlyingActor.count == 1)
      }
      "work on calling thread dispatcher" in {
        val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
       // val counter = system.actorOf(Props[Counter])
        val prob = TestProbe()
        prob.send(counter,Read)
        prob.expectMsg(Duration.Zero,0)// prob has already received the message


      }
    }


}
object SynchronousTestingSpec{
  case object Inc
  case object Read
  class Counter extends Actor{
      var count = 0

    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
