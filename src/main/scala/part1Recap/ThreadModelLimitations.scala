package part1Recap

object ThreadModelLimitations extends App {

  /*
  *  TML #1  OOP encapsulation is only valid in the SINGLE THREADED MODEL
  *
  * */

  class BankAccount(private var amount:Int){
    override def toString: String = ""+amount

    def withDraw(money:Int)= this.amount -= money
    def deposite(money:Int)= this.amount += money
    def getAmount = amount
  }

  val account  = new BankAccount(2000)
//  for(_<-1 to 100){
//    new Thread(()=>account.withDraw(1)).start()
//  }
//  for(_<-1 to 100){
//    new Thread(()=>account.deposite(1)).start()
//  }

  println(account.getAmount)

  // OOP encapsulation is broken in multiThreaded env
  // Syncronization => Locks


  /*
  *  TML 2 : Delegating something to a thread is a PAIN
  *
  *  you have a running thread and you want to pass a runnable to that thread? How to do
  *  */
  var task: Runnable = null
  val runningThread :Thread = new Thread(()=>{
    while (true){
      while (task == null){
        runningThread.synchronized{
          println("[BackGround] Waiting for a task")
          runningThread.wait()
        }
      }
      task.synchronized{
        println("[BackGround] I have a task")
        task.run()
        task = null
      }
    }
  })
def delegateToBackGround(r:Runnable) = {
  if(task == null) task = r
  runningThread.synchronized{
    runningThread.notify()
  }
}
  runningThread.start()
  Thread.sleep(1000)
  delegateToBackGround(()=>println("2323"))
  Thread.sleep(1000)
  delegateToBackGround(()=>println("This run in background"))

  /*
  *  TML #3  Tracing and dealing with errors in a multithreaded env is a PAIN
  * */
  // 1M numbers in between 10 Thread
  import scala.concurrent.ExecutionContext.Implicits.global


}
