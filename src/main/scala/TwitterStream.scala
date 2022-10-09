package fr.totetmatt.mediamur

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import com.twitter.clientlib.model.{AddOrDeleteRulesRequest, AddRulesRequest, AnimatedGif, DeleteRulesRequest, DeleteRulesRequestDelete, Photo, RuleNoId, StreamingTweetResponse, Video}
import spray.json.DefaultJsonProtocol
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._

import java.io.{BufferedReader, InputStreamReader, InterruptedIOException}
import java.util
import scala.jdk.CollectionConverters._

case class MediamurEntity(
                         id :String,
                         kind:String,
                         preview_url:String,
                         url:String
                         )
class TwitterStream {
  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  val apiInstance = new TwitterApi(new TwitterCredentialsBearer("AAAAAAAAAAAAAAAAAAAAALRxdgAAAAAAT1Bg1sxwy9RnxqEgJM2EMZ30%2B7M%3DHAwPxBnSc9MUFpxmT1W1S6qlc2j1qhgnqPYErWZM6xRrFazCGy"));

  var running: Boolean = false;
  var streamThread:Thread = null;
  val (tweetsQueueMat, tweetQueueSource) = Source.queue[MediamurEntity](10000, OverflowStrategy.backpressure).preMaterialize()

  val tweetdb : collection.mutable.HashMap[String,StreamingTweetResponse] = collection.mutable.HashMap.empty
  val mediaidToTweetid : collection.mutable.HashMap[String,Set[String]] = collection.mutable.HashMap.empty

  def getRules() = apiInstance.tweets().getRules.execute().toJson
  def addRule(tag:String, value:String) = {
    val rule = new RuleNoId();
    rule.setTag(tag)
    rule.setValue(value)
    val add = new AddRulesRequest();
    add.addAddItem(rule)
    val req = new AddOrDeleteRulesRequest(add);
    apiInstance.tweets().addOrDeleteRules(req).execute().toJson
  }
  def deleteRule(id:String) = {
    val deleteItems = new DeleteRulesRequestDelete()
    deleteItems.addIdsItem(id)
    val delete  = new DeleteRulesRequest();
    delete.setDelete(deleteItems)
    val req = new AddOrDeleteRulesRequest(delete);
    apiInstance.tweets().addOrDeleteRules(req).execute().toJson
  }
  def process(tweet: StreamingTweetResponse) : Seq[MediamurEntity] = {
    if (tweet != null && tweet.getIncludes != null  && tweet.getIncludes.getMedia!= null) {
      tweet.getIncludes.getMedia.asScala.map {
        case media if media.getType == "video" =>
          val  v = media.asInstanceOf[Video]
          val videoUrl = v.getVariants.asScala.filter(_.getBitRate != null).sortBy(_.getBitRate).map(_.getUrl.toString).last
          MediamurEntity(media.getMediaKey, media.getType,v.getPreviewImageUrl.toExternalForm,videoUrl)
        case media if media.getType == "animated_gif" =>
          val v = media.asInstanceOf[AnimatedGif]
          val gifUrl = v.getVariants.asScala.head.getUrl.toExternalForm
          MediamurEntity(media.getMediaKey, media.getType, v.getPreviewImageUrl.toExternalForm, gifUrl)
        case media if media.getType == "photo" =>
          val v = media.asInstanceOf[Photo]
          MediamurEntity(media.getMediaKey, media.getType, v.getUrl.toExternalForm, v.getUrl.toExternalForm)
      }.toSeq
    } else {
      Seq()
    }
  }
  def stop(): Unit ={
    running=false
    streamThread.interrupt()
  }
  def start(): Unit = {
    if(!running) {
      running=true
      streamThread = new Thread {
        override def run {
          val inputStream = apiInstance
            .tweets()
            .searchStream()
            ///.sampleStream()
            .expansions(util.Set.of("attachments.media_keys"))
            .tweetFields(util.Set.of("attachments", "entities"))
            .mediaFields(util.Set.of("preview_image_url", "variants", "url", "duration_ms", "height", "width", "media_key", "type"))
            .execute()
          try {
            val reader = new BufferedReader(new InputStreamReader(inputStream))
            try while (running) {
              val line = reader.readLine
              val tweet = StreamingTweetResponse.fromJson(line)
              tweetdb.put(tweet.getData.getId, tweet)
              val out = process(tweet)
              out.foreach(media => {
                val current : Set[String] = mediaidToTweetid.getOrElse(media.id, Set.empty[String])
                println(media.id)
                mediaidToTweetid.put(media.id,current + tweet.getData.getId)
                tweetsQueueMat.offer(media)
              })

            }
            catch {
              case e: InterruptedIOException =>
                e.printStackTrace()
              case e: Exception => println(e)
            } finally if (reader != null) reader.close()
          }
        }
      }
      streamThread.start()
    }

  }









}
