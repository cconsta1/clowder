package services.mongodb

import services.PreviewService
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import java.io.{InputStreamReader, BufferedReader, InputStream}
import play.api.Play._
import play.api.Logger
import com.mongodb.casbah.gridfs.GridFS
import models._
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntity}
import org.apache.http.entity.mime.content.StringBody
import java.nio.charset.Charset
import org.apache.http.util.EntityUtils
import scala.Some
import com.mongodb.casbah.WriteConcern

/**
 * Created by lmarini on 2/17/14.
 */
class MongoDBPreviewService extends PreviewService {

  def get(previewId: String): Option[Preview] = {
    PreviewDAO.findOneById(new ObjectId(previewId))
  }

  def setIIPReferences(id: String, iipURL: String, iipImage: String, iipKey: String) {
    PreviewDAO.dao.update(MongoDBObject("_id" -> new ObjectId(id)), $set("iipURL" -> Some(iipURL), "iipImage" -> Some(iipImage), "iipKey" -> Some(iipKey)), false, false, WriteConcern.Safe)
  }

  def findByFileId(id: String): List[Preview] = {
    PreviewDAO.dao.find(MongoDBObject("file_id"-> new ObjectId(id))).toList
  }

  def findBySectionId(id: String): List[Preview] = {
    PreviewDAO.dao.find(MongoDBObject("section_id"-> new ObjectId(id))).toList
  }

  def findByDatasetId(id: String): List[Preview] = {
    PreviewDAO.dao.find(MongoDBObject("dataset_id"-> new ObjectId(id))).toList
  }

  /**
   * Save blob.
   */
  def save(inputStream: InputStream, filename: String, contentType: Option[String]): String = {
    val files = current.plugin[MongoSalatPlugin] match {
      case None    => throw new RuntimeException("No MongoSalatPlugin");
      case Some(x) =>  x.gridFS("previews")
    }
    val mongoFile = files.createFile(inputStream)
    Logger.debug("Uploading file " + filename)
    mongoFile.filename = filename
    var ct = contentType.getOrElse(play.api.http.ContentTypes.BINARY)
    if (ct == play.api.http.ContentTypes.BINARY) {
      ct = play.api.libs.MimeTypes.forFileName(filename).getOrElse(play.api.http.ContentTypes.BINARY)
    }
    mongoFile.contentType = ct
    mongoFile.save
    mongoFile.getAs[ObjectId]("_id").get.toString
  }

  /**
   * Get blob.
   */
  def getBlob(id: String): Option[(InputStream, String, String, Long)] = {
    val files = GridFS(SocialUserDAO.dao.collection.db, "previews")
    files.findOne(MongoDBObject("_id" -> new ObjectId(id))) match {
      case Some(file) => Some(file.inputStream,
        file.getAs[String]("filename").getOrElse("unknown-name"),
        file.getAs[String]("contentType").getOrElse("unknown"),
        file.getAs[Long]("length").getOrElse(0))
      case None => None
    }
  }

  /**
   * Add annotation to 3D model preview.
   */
  def annotation(id: String, annotation: ThreeDAnnotation) {
    PreviewDAO.dao.update(MongoDBObject("_id" -> new ObjectId(id)), $addToSet("annotations" -> ThreeDAnnotation.toDBObject(annotation)), false, false, WriteConcern.Safe)
  }

  def findAnnotation(preview_id: String, x_coord: String, y_coord: String, z_coord: String): Option[ThreeDAnnotation] = {
    PreviewDAO.dao.findOneById(new ObjectId(preview_id)) match{
      case Some(preview) => {
        for(annotation <- preview.annotations){
          if(annotation.x_coord.equals(x_coord) && annotation.y_coord.equals(y_coord) && annotation.z_coord.equals(z_coord))
            return Option(annotation)
        }
        return None
      }
      case None => return None
    }
  }

  def updateAnnotation(preview_id: String, annotation_id: String, description: String){
    PreviewDAO.dao.findOneById(new ObjectId(preview_id)) match{
      case Some(preview) => {
        //var newAnnotations = List.empty[ThreeDAnnotation]
        for(annotation <- preview.annotations){
          if(annotation.id.toString().equals(annotation_id)){
            PreviewDAO.update(MongoDBObject("_id" -> new ObjectId(preview_id), "annotations._id" -> annotation.id) , $set("annotations.$.description" -> description), false, false, WriteConcern.Safe)
            return
          }
        }
        return
      }
      case None => return
    }
  }


  def listAnnotations(preview_id: String): List[ThreeDAnnotation] = {
    PreviewDAO.dao.findOneById(new ObjectId(preview_id)) match{
      case Some(preview) => {
        return preview.annotations
      }
      case None => return List.empty
    }
  }

  def removePreview(p: Preview){
    for(tile <- TileDAO.findByPreviewId(p.id)){
      TileDAO.remove(MongoDBObject("_id" -> tile.id))
    }
    // for IIP server references, also delete the files being referenced on the IIP server they reside
    if(!p.iipURL.isEmpty){
      val httpclient = new DefaultHttpClient()
      val httpPost = new HttpPost(p.iipURL.get+"/deleteFile.php")
      val entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
      entity.addPart("key", new StringBody(p.iipKey.get, "text/plain",
        Charset.forName( "UTF-8" )))
      entity.addPart("file", new StringBody(p.iipImage.get, "text/plain",
        Charset.forName( "UTF-8" )))
      httpPost.setEntity(entity)
      val imageUploadResponse = httpclient.execute(httpPost)
      Logger.info(imageUploadResponse.getStatusLine().toString())

      val dirEntity = imageUploadResponse.getEntity()
      Logger.info("IIP server: " + EntityUtils.toString(dirEntity))
    }

    if(!p.filename.isEmpty)
    // for oni previews, read the ONI frame references from the preview file and remove them
      if(p.filename.get.endsWith(".oniv")){
        val theFile = getBlob(p.id.toString())
        val frameRefReader = new BufferedReader(new InputStreamReader(theFile.get._1))
        var fileData = new StringBuilder()
        var currLine = frameRefReader.readLine()
        while(currLine != null) {
          fileData.append(currLine)
          currLine = frameRefReader.readLine()
        }
        frameRefReader.close()
        val frames = fileData.toString().split(",",-1)
        var i = 0
        for(i <- 0 to frames.length - 2){
          PreviewDAO.remove(MongoDBObject("_id" -> new ObjectId(frames(i))))
        }
        //same for PTM file map references
      }else if(p.filename.get.endsWith(".ptmmaps")){
        val theFile = getBlob(p.id.toString())
        val frameRefReader = new BufferedReader(new InputStreamReader(theFile.get._1))
        var currLine = frameRefReader.readLine()
        while(currLine != null) {
          if(!currLine.equals(""))
            PreviewDAO.remove(MongoDBObject("_id" -> new ObjectId(currLine.substring(currLine.indexOf(": ")+2))))
          currLine = frameRefReader.readLine()
        }
        frameRefReader.close()
      }

    PreviewDAO.remove(MongoDBObject("_id" -> p.id))
  }
}
