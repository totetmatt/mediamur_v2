package fr.totetmatt.mediamur


import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import com.twitter.clientlib.model.{AddOrDeleteRulesRequest, AddRulesRequest, AnimatedGif, DeleteRulesRequest, DeleteRulesRequestDelete, Photo, RuleNoId, StreamingTweetResponse, Video}
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedReader, InputStreamReader, InterruptedIOException}
import java.util
import scala.jdk.CollectionConverters._

case class MediamurEntity(
                         id :String,
                         kind:String,
                         preview_url:String,
                         url:String,
                         hit:Int
                         )
class TwitterStream {
  val logger: Logger = LoggerFactory.getLogger(this.getClass.toString)
  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  var config: MediamurConfig = Settings(system)
  // ""
  val apiInstance = new TwitterApi(new TwitterCredentialsBearer(config.BEARER_KEY));
  var running: Boolean = false;
  var streamThread:Thread = null;
  val (tweetsQueueMat, tweetQueueSource) = Source.queue[MediamurEntity](10000, OverflowStrategy.backpressure).preMaterialize()

  val tweetdb : collection.mutable.HashMap[String,StreamingTweetResponse] = collection.mutable.HashMap.empty
  val mediaidToTweetid : collection.mutable.HashMap[String,Set[String]] = collection.mutable.HashMap.empty

  def getRules: String = apiInstance.tweets().getRules.execute().toJson
  def addRule(tag:String, value:String): String = {
    val rule = new RuleNoId();
    rule.setTag(tag)
    rule.setValue(value)
    val add = new AddRulesRequest();
    add.addAddItem(rule)
    val req = new AddOrDeleteRulesRequest(add);
    apiInstance.tweets().addOrDeleteRules(req).execute().toJson
  }
  def deleteRule(id:String): String = {
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
          MediamurEntity(media.getMediaKey, media.getType,v.getPreviewImageUrl.toExternalForm,videoUrl, 1)
        case media if media.getType == "animated_gif" =>
          val v = media.asInstanceOf[AnimatedGif]
          val gifUrl = v.getVariants.asScala.head.getUrl.toExternalForm
          MediamurEntity(media.getMediaKey, media.getType, v.getPreviewImageUrl.toExternalForm, gifUrl, 1)
        case media if media.getType == "photo" =>
          val v = media.asInstanceOf[Photo]
          MediamurEntity(media.getMediaKey, media.getType, v.getUrl.toExternalForm, v.getUrl.toExternalForm,1)
      }.map(media=> {
        val hit = mediaidToTweetid.get(media.id).map(_.size).getOrElse(0)
        media.copy(hit=hit+1)
      }).toSeq
    } else {
      Seq()
    }
  }
  def stop(): Unit ={
    running = false
    streamThread.interrupt()
  }
  def start(isSample:Boolean): Unit = {
    if(!running) {
      running=true
      streamThread = new Thread {
        override def run(): Unit = {
          val inputStream = if(isSample) {
            apiInstance
              .tweets()
              .sampleStream()
              .expansions(util.Set.of("attachments.media_keys", "author_id", "entities.mentions.username"))
              .userFields(util.Set.of("name", "username", "id"))
              .tweetFields(util.Set.of("attachments", "entities", "text", "id"))
              .mediaFields(util.Set.of("preview_image_url", "variants", "url", "duration_ms", "height", "width", "media_key", "type"))
              .execute()
          } else {
            apiInstance
              .tweets()
              .searchStream()
              .expansions(util.Set.of("attachments.media_keys", "author_id", "entities.mentions.username"))
              .userFields(util.Set.of("name", "username", "id"))
              .tweetFields(util.Set.of("attachments", "entities", "text", "id"))
              .mediaFields(util.Set.of("preview_image_url", "variants", "url", "duration_ms", "height", "width", "media_key", "type"))
              .execute()
          }
          try {
            val reader = new BufferedReader(new InputStreamReader(inputStream))
            try while (running) {
              try {
                val line = reader.readLine
                val tweet = StreamingTweetResponse.fromJson(line)
                tweetdb.put(tweet.getData.getId, tweet)
                val out = process(tweet)
                out.foreach(media => {
                  val current : Set[String] = mediaidToTweetid.getOrElse(media.id, Set.empty[String])
                  mediaidToTweetid.put(media.id,current + tweet.getData.getId)
                  tweetsQueueMat.offer(media)
                })
              } catch {
                case e: NullPointerException => logger.warn(e.getMessage)
                case e:IllegalArgumentException => logger.warn(e.getMessage)
              }
            }
            catch {
              case e: InterruptedIOException => logger.warn(e.getMessage)
              case e: Exception => logger.warn(e.getMessage)
            } finally if (reader != null) reader.close()
          }
        }
      }
      streamThread.start()
    }

  }









}
