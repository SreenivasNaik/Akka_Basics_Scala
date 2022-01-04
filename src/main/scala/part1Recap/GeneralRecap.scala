package part1Recap

import java.util.UUID
import scala.util.Try

object GeneralRecap extends App {

  val aCondition:Boolean = false
  // aCondition = true  val is not able to reaasign

  var aVariable = 42
  aVariable+1

  // expressions
  val aConditionVal = if(aCondition) 42 else 32

  // code blocks
  val aCodeBlock = {
    if(aCondition) 42
    22
  }

  // Types ->Unit --> void
  def aFunction(x:Int):Int = x+1

  // Recursion -> Tail recursion
  def factorial(n:Int,acc:Int): Int =
    if(n<=0) acc
    else factorial(n-1,acc*n)

  // OOP
  class Animal
  class Dog extends Animal
  val aDog:Animal = new Dog

  trait Carnivore{
    def eat(a:Animal):Unit
  }
  class Crocodile extends Animal with Carnivore{
    override def eat(a: Animal): Unit = println("HHH")
  }

  // Method Notations
  val aCroc = new Crocodile
  aCroc.eat(aDog)
  aCroc eat aDog

  // Anonymous classes
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("adhad")
  }

  // Generics
  abstract class MyList[+A]

  // companion object
  object MyList

  // case classes
    case class Person(name:String,age:Int)

  // Exceptions
  val aPotentialFailure = try{
    throw new RuntimeException("hhah") // NOthing
  }catch {
    case e:Exception => "hello"
  }finally {
    println("some logs")
  }

  // Functional Programming
  val incrementar = new Function[Int,Int] {
    override def apply(v1: Int): Int = v1+1
  }
  val incremental = incrementar(32)

  // Syntax sugar
  val anonymousInremental = (x:Int) => x+1

  List(1,2,3).map(incrementar)
    // HOF

  // for comprehensions

  val pair = for {
    num <- List(1,2,3,4)
    char <- List("a","b")
  } yield num+"_"+char
  // List(1,2,3,4).flatmap(num=>List("a","b").map(char=> num+"_"+char))


  //Options and Try

  val anOption = Some(2)
  val aTry = Try
  {
    throw new RuntimeException

  }
  // Pattern matching

  val unknown = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "seconds"
  }

  val agent2Id = UUID.randomUUID()
  //println((agent2Id == agent2Id))
  println((agent2Id != agent2Id))

}
