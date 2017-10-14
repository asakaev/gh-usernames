package ru.vf8

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Success

object Application extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val pool = Http().cachedHostConnectionPoolHttps[String]("github.com")
  val start = System.nanoTime()

  val names = ('a' to 'z').toSet
    .subsets(3)
    .map(_.mkString)
    .map(name => (HttpRequest(HttpMethods.HEAD, s"/$name"), name))
    .toList

  println(s"Total names: ${names.size}")

  val fileSink = FileIO.toPath(Paths.get("target/names3.txt"))

  Source(names)
    .via(pool)
    .collect {
      case (Success(r), name) if r.status != StatusCodes.OK =>
        ByteString(name + '\n')
    }
    .runWith(fileSink)
    .onComplete(done => {
      val stop = System.nanoTime()
      val time = Duration.fromNanos(stop - start)
      println(s"Seconds: ${time.toSeconds}")
      println(done)
    })
}