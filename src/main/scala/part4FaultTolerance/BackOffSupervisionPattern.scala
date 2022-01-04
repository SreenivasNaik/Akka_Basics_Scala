package part4FaultTolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import java.io.File
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object BackOffSupervisionPattern extends App {

  case object ReadFile
  class FilebasedPersistanceActor extends Actor with ActorLogging{
    var dataSource:Source = null

    override def preStart(): Unit = {
      log.info("FilebasedPersistanceActor ==> Starting ")
    }

    override def postStop(): Unit = log.info("FilebasedPersistanceActor ==> Stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("FilebasedPersistanceActor ==> Restarting")
    override def receive: Receive = {
      case ReadFile =>
        if(dataSource == null)
            dataSource = Source.fromFile(new File("src/main/resources/testfiles/importance_data.txt"))
        log.info("I have read the data from file"+dataSource.getLines().toList)
    }

  }
  val system =ActorSystem("BackOffDemo")
  val simpleActor = system.actorOf(Props[FilebasedPersistanceActor],"simpleActor")
  //simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FilebasedPersistanceActor],
      "SimpleBackOffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

 // val simpleBackOffSup = system.actorOf(simpleSupervisorProps,"simpleSupervisor")

  /* simpleSupervisor
  *     -> it create a child called simpleBackoffActor with props of type FileBased actor
  *     -> supervison strategy is default one --> restarting on everything
  *         -> First attempt after 3 seconds
  *          -> when fails again Next atempt 2*X times of previous atempt => 6s 12s 24s
  *          ->
  * */
  //simpleBackOffSup ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FilebasedPersistanceActor],
      "StopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy(){
        case _ => Stop
      }
    )
  )
//  val stopBackOffSup = system.actorOf(stopSupervisorProps,"StopSupervisor")
//  stopBackOffSup ! ReadFile

  class EgarFileBasedActor extends FilebasedPersistanceActor{
    override def preStart(): Unit = {
      log.info("EgarFileBasedActor==>  Starting ")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/importance_data.txt"))
    }
  }

//  val egarFileBasedActor = system.actorOf(Props[EgarFileBasedActor])

  // By Default the supervison strategy for akka.actor.ActorInitializationException ==> is STOP

  val repeadtedSuperVisionProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EgarFileBasedActor],
      "egarActor",
      1 second,
      30 seconds,
      0.1
    ))
  val egarSupFileBasedActor = system.actorOf(repeadtedSuperVisionProps,"EgarSup")

  /* EagarSupervisor
      -> child egarActor
        -> will die on start with akka.actor.ActorInitializationException
         -> Trigger supervison strategy => STOP
      -> BackOff will kick In 1s,2s,4s,8s...
  *
  * */
}
