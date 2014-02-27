package services.mongodb

import services.AppConfigurationService
import models.{MongoContext, AppConfiguration}
import scala.Some
import play.api.Logger
import com.novus.salat.dao.{ModelCompanion, SalatDAO}
import com.mongodb.casbah.Imports._
import MongoContext.context
import play.api.Play.current

/**
 * Created by lmarini on 2/21/14.
 */
class MongoDBAppConfigurationService extends AppConfigurationService {

  def getDefault(): Option[AppConfiguration] = {
    AppConfiguration.findOne(MongoDBObject("name" -> "default")) match {
      case Some(conf) => Some(conf)
      case None => {
        val default = models.AppConfiguration()
        AppConfiguration.save(default)
        Some(default)
      }
    }
  }

  def setTheme(theme: String) {
    Logger.debug("Setting theme to " + theme)
    getDefault match {
      case Some(conf) => AppConfiguration.update(MongoDBObject("name" -> "default"), $set("theme" -> theme), false, false, WriteConcern.Safe)
      case None => {}
    }
  }

  def getTheme(): String = {
    getDefault match {
      case Some(appConf) => Logger.debug("Theme" + appConf.theme); appConf.theme
      case None => themes(0)
    }
  }
}

object AppConfiguration extends ModelCompanion[AppConfiguration, ObjectId] {
  val dao = current.plugin[MongoSalatPlugin] match {
    case None => throw new RuntimeException("No MongoSalatPlugin");
    case Some(x) => new SalatDAO[AppConfiguration, ObjectId](collection = x.collection("app.configuration")) {}
  }
}
