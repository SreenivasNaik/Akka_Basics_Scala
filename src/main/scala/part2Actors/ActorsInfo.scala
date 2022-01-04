package part2Actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsInfo extends App {
  // Part 1 - > create a ActorSystem
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // Part 2 - Create a Actors
  // word count actor

  class WordCountActor extends Actor{
    // Internal Data
    var wordCount = 0
    override def receive: PartialFunction[Any,Unit] = {
      // Behavior
      case message:String =>
        println(s"[WordCount Actor] I have received message:$message")
        wordCount += message.split(" ").length
      case msg => println("[WordCount Actor] I am not able to understand"+msg.toString)
    }
  }

  // Part 3 Instantiate Actors

  val wordCounter = actorSystem.actorOf(Props[WordCountActor],"wordCounter") // It will give ActorRef
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor],"anotherWordCounter") // It will give ActorRef

    // Part 4 - Communicate
  wordCounter ! "I am learning akka"
  anotherWordCounter ! "A different word"
  // Asynchronus

  // Best Practice is Create a factory method in companion object
  object Person{
    def props(name:String) = Props(new Person(name))
  }
  class Person(name:String) extends Actor{
    override def receive: Receive = {
      case msg:String => println(s"I am $name saying Hi to $msg")
    }
  }

  // How to instantiate the classes with parameters
 val person = actorSystem.actorOf(Person.props("Sreenu"),"Person")

person ! "Hi"




}
