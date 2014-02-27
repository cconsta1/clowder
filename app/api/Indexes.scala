package api

import play.api.mvc.Controller
import play.api.libs.json.JsObject
import com.mongodb.casbah.Imports._
import play.api.libs.json.Json._
import play.api.Logger
import models.Feature
import models.MultimediaFeatures
import models.PreviewDAO
import play.api.Play.current
import services.{MultimediaQueryService, RabbitmqPlugin, ExtractorMessage, VersusPlugin}
import javax.inject.Inject

/**
 * Index data.
 * 
 * @author Luigi Marini
 *
 */
@Inject
class Indexes @Inject() (multimediaSearch: MultimediaQueryService) extends Controller with ApiController {

  /**
   * Submit section, preview, file for indexing.
   */
  def index() = SecuredAction(authorization=WithPermission(Permission.AddIndex)) { request =>
      (request.body \ "section_id").asOpt[String].map { section_id =>
      	  (request.body \ "preview_id").asOpt[String].map { preview_id =>
      	    PreviewDAO.findOneById(new ObjectId(preview_id)) match {
      	      case Some(p) =>
	      	    // TODO RK need to replace unknown with the server name
	            val key = "unknown." + "index."+ p.contentType.replace(".", "_").replace("/", ".")
	            // TODO RK : need figure out if we can use https
	            val host = "http://" + request.host + request.path.replaceAll("api/indexes$", "")
	            val id = p.id.toString
	            current.plugin[RabbitmqPlugin].foreach{
	              _.extract(ExtractorMessage(id, id, host, key, Map("section_id"->section_id), p.length.toString, "", ""))}
	            var fileType = p.contentType
	            current.plugin[VersusPlugin].foreach{ _.indexPreview(id,fileType) }
	            Ok(toJson("success"))
      	      case None => BadRequest(toJson("Missing parameter [preview_id]"))
            }
      	   
      	  }.getOrElse {
      		BadRequest(toJson("Missing parameter [preview_id]"))
      	  }
      }.getOrElse {
        BadRequest(toJson("Missing parameter [section_id]"))
      }
  }
  
  /**
   * Add feature to index.
   */
  def features() = SecuredAction(authorization=WithPermission(Permission.AddIndex)) { request =>
      (request.body \ "section_id").asOpt[String].map { section_id =>
        multimediaSearch.findFeatureBySection(section_id) match {
          case Some(multimediaFeature) => {
            val features = (request.body \ "features").as[List[JsObject]]
            multimediaSearch.updateFeatures(multimediaFeature, section_id, features)
            Ok(toJson(Map("id"->multimediaFeature.id.toString)))
          }
          case None => {
            val jsFeatures = (request.body \ "features").as[List[JsObject]]
            val features = jsFeatures.map {f =>
            	Feature((f \ "representation").as[String], (f \ "descriptor").as[List[Double]])
            }
            val doc = MultimediaFeatures(section_id = Some(new ObjectId(section_id)), features = features)
            multimediaSearch.insert(doc)
            Ok(toJson(Map("id"->doc.id.toString)))
          }
        }
      }.getOrElse {
        BadRequest(toJson("Missing parameter [section_id]"))
      }
  }
}
