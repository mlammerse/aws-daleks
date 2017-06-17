package aws.daleks

import com.amazonaws.regions.Region
import com.amazonaws.AmazonWebServiceClient
import rx.lang.scala._
import com.amazonaws.regions.Regions
import aws.lotr._
import java.util.ArrayList
import java.util.List
import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.Failure
import scala.util.Success

abstract class RxDalek[T](implicit region: Region) extends Dalek {
  val extra = scala.collection.mutable.Map[String, String]()
  if (region != null) extra += ("region" -> region.toString())

  def observe: Observable[T] = list().asScala.toObservable

  def list(): List[T] = new ArrayList()
  def exterminate(t: T): Unit = {}
  def describe(t: T): Map[String, String] = Map()

  def flyDependencies(t: T) = {}

  def mercy(t: T) = false

  def fly = for (target <- observe) {
    flyDependencies(target)
    val description = describe(target)
    val result =
      if (mercy(target)) "mercy"
      else if (Dalek.good) "good"
      else Try {
        exterminate(target)
      } match {
        case Success(s) => "exterminated"
        case Failure(e) => {
          e.printStackTrace()
          s"failed[${e.getMessage}]"
        }
      }
    speak(description + ("result" -> result))
  }

  def speak(description: Map[String, String]): Unit = {
    println((description ++ extra)
      .toSeq
      .sortWith {
        case ((a1, a2), (b1, b2)) =>
          if ("region".equals(a1)) true
          else if ("region".equals(b1)) false
          else a1 < b1
      }
      .map { case (key, value) => s"${key}=${value}" }
      .mkString(", "))
  }

  def isLOTR(name: String) = !locations.find { loc =>
    name.toUpperCase.startsWith(loc)
  }.isEmpty

  def isDND(name: String) = name.toUpperCase.contains("DO-NOT-DELETE")
}