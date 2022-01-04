package playGround

import akka.actor.ActorSystem

object PlayGround extends App {

  val actorSystem = ActorSystem("Sreenivas_ActorSystem")
  println(actorSystem.name)


}
