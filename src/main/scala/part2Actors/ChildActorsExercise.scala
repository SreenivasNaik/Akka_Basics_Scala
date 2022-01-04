package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {

   // Distributing word count
  object WordCountMaster{
     case class Initialize(nChildren:Int)
     case class WordCountTask(id:Int,text:String)
     case class WordCountReply(id:Int,count:Int)

   }
  class WordCountMaster extends Actor {
    import WordCountMaster._
     override def receive: Receive = {
       case Initialize(nChildren) =>
         println(s"[Master] Initialize")
         val childRefs = for( i <- 1 to nChildren) yield  context.actorOf(Props[WordCountWorker],s"WCW_$i")
         context.become(withChildren(childRefs,0,0,Map()))
     }
    def withChildren(childRefs: Seq[ActorRef],currentChildIndex:Int,currentTaskId:Int,requestMap:Map[Int,ActorRef]):Receive = {
      case text:String =>
        println(s"[master] i have received $text I will send it to child:$currentChildIndex")
        val originalSender = sender()
          val task = WordCountTask(currentTaskId ,text)
          val childRef = childRefs(currentChildIndex)
        childRef ! task
        val nextChild = (currentChildIndex+1) % childRefs.length
        val newTaskId = currentTaskId+1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childRefs,nextChild,newTaskId,newRequestMap))

      case WordCountReply(id,count) =>
        println(s"[master] I have received reply for taskId $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childRefs,currentChildIndex,currentTaskId, requestMap - id))
    }
   }
  class WordCountWorker extends Actor{
    import WordCountMaster._
    override def receive: Receive = {
      case WordCountTask(id,text) =>
        println(s"${self.path} I have recived task $id with $text")
        sender() ! WordCountReply(id,text.split(" ").length)
    }
  }
  /* Create a WordCountMaster
    send Initialize(10) to wordcountmaster => create 10 wordcountworkers
    send "Srenu cna" to wcm
      -> wcm will send WordCountTask("...") to one of its childer
      => child will respond back with WordCountReplay
        master replies that to sender

        Round Robin Logic
  *
  * */

  class TestActor extends Actor{
    import  WordCountMaster._
    override def receive: Receive = {
      case "go" =>
         val master  = context.actorOf(Props[WordCountMaster],"master")
        master ! Initialize(3)
        val texts = List("I love akka","Super Sreenu","Naik","cnu shd shjd")
        texts.foreach(texts => master ! texts)
      case count:Int =>
        println(s"[Test Actor] I received a replay :$count")
    }
  }
  val system = ActorSystem("RoundRobin")

  val testActor = system.actorOf(Props[TestActor],"testActor")
  testActor ! "go"

}
