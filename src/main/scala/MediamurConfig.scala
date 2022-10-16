package fr.totetmatt.mediamur

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

class MediamurConfig(config: Config) extends Extension {
  val BEARER_KEY: String = config.getString("mediamur.twitter.bearer_key")


  val WEB_SERVER_HOST: String = config.getString("akka.http.server.host")
  val WEB_SERVER_PORT: Int = config.getInt("akka.http.server.port")
}
object Settings extends ExtensionId[MediamurConfig] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new MediamurConfig(system.settings.config)

  override def get(system: ActorSystem): MediamurConfig = super.get(system)
}