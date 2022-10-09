package fr.totetmatt.mediamur
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Sink
import spray.json.{DefaultJsonProtocol, enrichAny}

import scala.concurrent.duration.DurationInt
import scala.io.StdIn

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val rulesEntityFormat = jsonFormat3(Rules)
  implicit val mediamurEntityFormat = jsonFormat4(MediamurEntity)
}
case class Rules (
  id:String,
  tag:String,
  value:String
  )
object Main extends JsonSupport {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext
    val stream = new TwitterStream()
    stream.start()
    stream.tweetQueueSource.runWith(Sink.ignore)
    val route =
      path("rules") {
        get {
          complete(HttpEntity(ContentTypes.`application/json`,  stream.getRules))
        } ~ post { entity(as[Rules]) { rules =>
            if(rules.tag.nonEmpty && rules.value.nonEmpty) {
                val result = stream.addRule(rules.tag,rules.value)

                complete(HttpEntity(ContentTypes.`application/json`,   result.toJson.toString))
            } else {
              complete(HttpEntity(ContentTypes.`application/json`, "{}"))
            }
          }
        } ~ delete {entity(as[Rules]) { rules =>
            if (rules.id.nonEmpty) {
              val result = stream.deleteRule(rules.id)
              complete(HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
            } else {
              complete(HttpEntity(ContentTypes.`application/json`, "{}"))
            }
          }
        }
      } ~ path("events") {
        get {
          complete {
            stream.tweetQueueSource
              .map(media => ServerSentEvent(data = media.toJson.toString(), eventType = Some(media.kind)))
              .keepAlive(1.second, () => ServerSentEvent.heartbeat)

          }
        }
      } ~ path("tweet" / Segment) { id:String=>
        get {
              complete {
                stream
                  .mediaidToTweetid
                  .get(id)
                  .map(tweetIds => tweetIds.flatMap(tweetId=>stream.tweetdb.get(tweetId)))
                  .map(_.map(_.toJson))
                  .getOrElse(Set.empty)
                  .toJson
              }
            }
      } ~ pathPrefix("") {
        getFromDirectory("./ui")
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }


}