package part1Recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {

  // Creating threads

  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("shdhads")
  })

  val aThread1 = new Thread(()=>println("sdsd"))

    aThread.start()
  aThread.join()

  val threadHelo = new Thread(()=>(1 to 100).foreach(_=>println("hellp")))
  val threadbye = new Thread(()=>(1 to 100).foreach(_=>println("bye")))

  threadHelo.start()
  threadbye.start()

  // Different runs different results

  // Synchronization


  //@volatile

  // Inter-thread communication
  // Wait - Notify

    // scala Futures
import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    // in different thread
    232
  }
  // call backs
  future.onComplete{
    case Success(value) => println("success")
    case Failure(exception) => print("Fail")
  }

  val aprocceedFuture = future.map(_+1)
  val aFlatFuture = future.flatMap(value=>
  Future(value+1))

  val filteredFuture = future.filter(_%2 ==0) // NoSuch Element exception

  // For comprehensions

  val forComprehensionFuture = for {
    meaning <- future
    filter <- filteredFuture
  } yield meaning+filter

  // andThen, Recover/ RecoverWith

  // Promises


}

