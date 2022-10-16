package fr.totetmatt.mediamur
import scala.jdk.CollectionConverters._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Sink
import com.twitter.clientlib.model.StreamingTweetResponse
import com.typesafe.config.ConfigFactory
import spray.json.{DefaultJsonProtocol, enrichAny}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
case class User(user_name:String, user_login:String)
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val rulesEntityFormat = jsonFormat3(Rules)
  implicit val mediamurEntityFormat = jsonFormat5(MediamurEntity)
  implicit val userEntityFormat = jsonFormat2(User)
  implicit val startFormat = jsonFormat1(Start)
}
case class Start(isSample:Boolean)
case class Rules(
                  id: String,
                  tag: String,
                  value: String
                )

object Main extends JsonSupport {
  def getUser(s :StreamingTweetResponse )  = {
    s.getIncludes
      .getUsers
      .asScala
      .find(_.getId ==  s.getData.getAuthorId).map(x=> User(x.getName,x.getUsername))
  }
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "mediamur")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    val settings = Settings(system)

    val stream = new TwitterStream()
    stream.start(false)
    stream.tweetQueueSource.runWith(Sink.ignore)
    val route =
      path("rules") {
        get {
          complete(HttpEntity(ContentTypes.`application/json`, stream.getRules))
        } ~ post {
          entity(as[Rules]) { rules =>
            if (rules.tag.nonEmpty && rules.value.nonEmpty) {
              val result = stream.addRule(rules.tag, rules.value)

              complete(HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
            } else {
              complete(HttpEntity(ContentTypes.`application/json`, "{}"))
            }
          }
        } ~ delete {
          entity(as[Rules]) { rules =>
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
      } ~ path("tweet" / Segment) { id: String =>
        get {
          complete {
            stream
              .mediaidToTweetid
              .get(id)
              .map(tweetIds => tweetIds.flatMap(tweetId => stream.tweetdb.get(tweetId)))
              .map(_.groupMap(_.getData.getText)(getUser))
              .map(_.map(_.toJson))
              .getOrElse(Set.empty)
              .toJson
          }
        }
      } ~ pathPrefix ("stream") {
        path("stop") {
          post {
            stream.stop()
            complete(HttpEntity.Empty)
          }
        } ~ path("start") {
          post {
            entity(as[Start]) { start =>
              stream.start(start.isSample)
              complete(HttpEntity.Empty)
            }
          }
        }
      } ~ pathPrefix("") {
        getFromDirectory("./ui")
      }

    val bindingFuture = Http().newServerAt(settings.WEB_SERVER_HOST, settings.WEB_SERVER_PORT).bind(route)

    println(s"http://${settings.WEB_SERVER_HOST}:${settings.WEB_SERVER_PORT}/index.html\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}