package part1Recap

import scala.concurrent.Future

object AdvanceRecap extends App {

  // PArtial Function => Functions operates

  val partialFunction :PartialFunction[Int,Int] = {
    case 1 =>23
    case 2 =>23
    case 11 =>23
  }

  val pd = (x:Int) => x match {
    case 1 => 12
    case 2 =>22
  }

  val fucntion:(Int=>Int) = partialFunction

  val modifiedList = List(1,2,3).map{
    case 1 =>23
    case _ =>0
  }

  // lifting
  val lifted = partialFunction.lift
  lifted(2)
  lifted(22)

  // orElse
  val pfCHain = partialFunction.orElse[Int,Int]{
    case 40=>223
  }

  // type alias

  type ReceiveFunction = PartialFunction[Any,Unit]

  def receive:ReceiveFunction ={
    case 1 =>2
  }

  // Implicits

  // implicits Conversions
   case class Person(name:String) {
    def greet = s"Hi my name is $name"
  }
  implicit def fromStringTOPerson(string: String):Person = Person(string)

  "Sreenu".greet

  // implicit classes
    implicit class Dog(name:String){
    def bark = println("Hh")
  }
  "lAA".bark

  // Organize the implicits val

  implicit val inverseOrder:Ordering[Int] = Ordering.fromLessThan(_>_)
  List(1,3,2).sorted

  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    println("shdks")
  }

  // companion objects of the type in the call

  object Person{
    implicit val personOrder:Ordering[Person] = Ordering.fromLessThan((a,b)=> a.name.compareTo(b.name)<0)
  }
  List(Person("adsd"),Person("zxzx")).sorted

}
