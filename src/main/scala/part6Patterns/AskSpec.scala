package part6Patterns


import akka.actor.TypedActor.context
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import part6Patterns.AskSpec.AuthManager.{AUTH_FAILURE_NOT_FOUND, AUTH_FAILURE_PASSWORD_INCORRECT, AUTH_FAILURE_SYSTEM_ERROR}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import  AskSpec._

  "An AuthenticationManager" should{
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! AuthenticateUser("Sreenu","sree")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }
    "Failed To authenticate if invalid password" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("Sreenu","Sreenu")
      authManager ! AuthenticateUser("Sreenu","sreenu")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
    "Sucessfully To authenticate user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("Sreenu","Sreenu")
      authManager ! AuthenticateUser("Sreenu","Sreenu")
      expectMsg(AuthSucess)
    }
  }
  "An PIPED AuthenticationManager" should{
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManger])
      authManager ! AuthenticateUser("Sreenu","sree")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }
    "Failed To authenticate if invalid password" in {
      val authManager = system.actorOf(Props[PipedAuthManger])
      authManager ! RegisterUser("Sreenu","Sreenu")
      authManager ! AuthenticateUser("Sreenu","sreenu")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
    "Sucessfully To authenticate user" in {
      val authManager = system.actorOf(Props[PipedAuthManger])
      authManager ! RegisterUser("Sreenu","Sreenu")
      authManager ! AuthenticateUser("Sreenu","Sreenu")
      expectMsg(AuthSucess)
    }
  }
}
object AskSpec {

  /*
  *
  * */
  case class Read(key: String)

  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to Read Value for Key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Updating the value $value for Key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(userName: String, password: String)

  case class AuthenticateUser(userName: String, password: String)

  case class AuthFailure(message: String)

  case object AuthSucess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "UserName Not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "Incorrect Password"
    val AUTH_FAILURE_SYSTEM_ERROR = "System Error"
  }



  class AuthManager extends Actor with ActorLogging {
    //STEP:2 Logistics
    implicit val timeOut: Timeout = Timeout(1 second)
    implicit val executionDispatcher = context.dispatcher

    protected val authDb = context.actorOf(Props[KVActor])
    override def receive: Receive = {
      case RegisterUser(userName, password) => authDb ! Write(userName, password)
      case AuthenticateUser(userName, password) => handleAuthentication(userName, password)
    }

    def handleAuthentication(userName: String, password: String) = {
      val originalSender = sender()
      // step:3 Ask the Actor
      val future = authDb ? Read(userName)
      // Step:4 handle the future
      future.onComplete {
        // STEP:5  NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACESS MUTABLE STATE IN ON COMPLETE
        // Avoid closing over the actor instance or mutable state
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword.equals(password)) originalSender ! AuthSucess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }
  class PipedAuthManger extends AuthManager {
    override def handleAuthentication(userName: String, password: String): Unit = {
      // ASK the actor
      val future = authDb ? Read(userName) // Future[Any]
      // Proccess the future until you get the responses you will send back
      val passwordFuture = future.mapTo[Option[String]]
      // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if(dbPassword == password) AuthSucess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // FUTURE[ANY]
        // PIPE
      // When the future completes send the  response to the actor ref in the arg list
      responseFuture.pipeTo(sender())
    }
  }
}
