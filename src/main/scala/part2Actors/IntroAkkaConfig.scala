package part2Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  /*
  *  1 . Inline Configuration
  * */
    val configString =
      """
        | akka {
        |   loglevel = "DEBUG"
        |   }
        |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo",ConfigFactory.load(config))

  class SimpleLoggingActor extends Actor with ActorLogging{
    override def receive: Receive ={
      case message => log.info(message.toString)
    }
  }
  val actor = system.actorOf(Props[SimpleLoggingActor],"actor")
  actor ! "message to remember"


  /*2 . Defaulut file config*/
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileSystem")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor],"defaultActor")

  defaultConfigActor ! "Remember me"

  /*
  3 . separate config in the same file
  * */
  val specialCOnfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigFileSystem = ActorSystem("SpecialConfigFileSystem",specialCOnfig)

  val specialConfigActor = specialConfigFileSystem.actorOf(Props[SimpleLoggingActor],"SpecialActor")

  specialConfigActor ! "Remember me specialConfigActor"

  /* 4 . Separate config in another file
  * */

  val separateConfig = ConfigFactory.load("secreat/secreatConfig.conf")
  println(s"SeparateConfig log level : ${separateConfig.getString("akka.loglevel")}")

  /* 5 . Different file formats
      JSOn ,Properties
  * */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"Json log level : ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfig.properties")
  println(s"props log level : ${propsConfig.getString("akka.loglevel")}")
}
