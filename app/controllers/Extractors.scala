package controllers

import models.{ExtractorInfo, Folder, ResourceRef, UUID}
import play.api.mvc.Controller
import api.Permission
import javax.inject.{Inject, Singleton}
import play.api.Logger
import services._

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer

import play.api.libs.ws.{Response, WS}

/**
 * Information about extractors.
 */
@Singleton
class Extractors  @Inject() (extractions: ExtractionService,
                             extractorService: ExtractorService,
                             fileService: FileService,
                             datasetService: DatasetService,
                             folders: FolderService,
                             spaces: SpaceService,
                             logService: LogService,
                             datasets: DatasetService ) extends Controller with SecuredController {

  def listAllExtractions = ServerAdminAction { implicit request =>
    implicit val user = request.user
    val allExtractions = extractions.findAll()
    Ok(views.html.listAllExtractions(allExtractions))
  }

  /**
    * Gets list of extractors from mongo. Displays the page to enable/disable extractors.
    */
  def selectExtractors() = AuthenticatedAction { implicit request =>
    implicit val user = request.user
    val runningExtractors: List[ExtractorInfo] = extractorService.listExtractorsInfo(List.empty)
    val selectedExtractors: List[String] = extractorService.getEnabledExtractors()
    Ok(views.html.updateExtractors(runningExtractors, selectedExtractors))
  }

  /**
    * Processes POST request. Updates list of extractors associated with this instance in mongo.
    */
  def updateExtractors() = AuthenticatedAction(parse.multipartFormData) { implicit request =>
    implicit val user = request.user

    // Bounce any non-admin users back to the homepage
    // TODO: is there a better pattern for this? I couldn't access "user" when specifying PermissionAction() above
    user match {
      case Some(u) => {
        if (!u.superAdminMode && !Permission.checkServerAdmin(user)) {
          Redirect(routes.Application.index())
        }
      }
      case None => Redirect(routes.Application.index())
    }

    //form contains space id and list of extractors.
    var extractors: List[String] = Nil

    extractorService.disableAllExtractors()

    val dataParts = request.body.dataParts
    // if extractors are selected, add them
    if (dataParts.isDefinedAt("extractors")) {
      extractors = dataParts("extractors").toList
      extractors.map(extractorService.enableExtractor(_))
    }
    Redirect(routes.Application.index())
  }

  def showExtractorLog(extractorName: String) = AuthenticatedAction { implicit request =>
    implicit val user = request.user
    val playConfig = play.Play.application().configuration()
    val serviceEndpoint = playConfig.getString("clowder.log.serviceEndpoint")
    Logger.debug(s"logging endpoint - $serviceEndpoint")
/*
    val futureResponse = WS.url(serviceEndpoint).get()
    val logs = logService.getLog()
    logs match {
      case Some() => Ok(view.html.extractorLog(logs))
      case None => InternalServerError("Extractor not found: " + extractorName)
    }
*/
/*
    val targetExtractor = extractorService.listExtractorsInfo(List.empty).find(p => p.name == extractorName)
    targetExtractor match {
      case Some(extractor) => Ok(views.html.extractorDetails(extractor))
      case None => InternalServerError("Extractor not found: " + extractorName)
    }
    */
  }

  def showExtractorInfo(extractorName: String) = AuthenticatedAction { implicit request =>
    implicit val user = request.user
    val targetExtractor = extractorService.listExtractorsInfo(List.empty).find(p => p.name == extractorName)
    targetExtractor match {
      case Some(extractor) => Ok(views.html.extractorDetails(extractor))
      case None => InternalServerError("Extractor not found: " + extractorName)
    }
  }

  def submitFileExtraction(file_id: UUID) = PermissionAction(Permission.EditFile, Some(ResourceRef(ResourceRef.file, file_id))) { implicit request =>
    implicit val user = request.user
    val all_extractors = extractorService.listExtractorsInfo(List("EXTRACT", "CONVERT"))
    val extractors = all_extractors.filter(!_.process.file.isEmpty)
    fileService.get(file_id) match {

      case Some(file) => {
        val foldersContainingFile = folders.findByFileId(file.id).sortBy(_.name)
        var folderHierarchy = new ListBuffer[Folder]()
        if(foldersContainingFile.length > 0) {
          folderHierarchy = folderHierarchy ++ foldersContainingFile
          var f1: Folder = folderHierarchy.head
          while (f1.parentType == "folder") {
            folders.get(f1.parentId) match {
              case Some(fparent) => {
                folderHierarchy += fparent
                f1 = fparent
              }
              case None =>
            }
          }
        }
        val datasetsContainingFile = datasets.findByFileIdDirectlyContain(file.id).sortBy(_.name)
        val allDatasets =  datasets.get(folders.findByFileId(file.id).map(_.parentDatasetId)).found ++ datasetsContainingFile
        val allDecodedDatasets = ListBuffer.empty[models.Dataset]
        val decodedSpacesContaining= ListBuffer.empty[models.ProjectSpace]
        for (aDataset <- allDatasets) {
          val dDataset = Utils.decodeDatasetElements(aDataset)
          allDecodedDatasets += dDataset
          aDataset.spaces.map{ sp =>
            spaces.get(sp) match {
              case Some(s) => {
                decodedSpacesContaining += Utils.decodeSpaceElements(s)
              }
              case None =>
            }
          }
        }
        Ok(views.html.extractions.submitFileExtraction(extractors, file, folderHierarchy.reverse.toList, decodedSpacesContaining.toList, allDecodedDatasets.toList))
      }
      case None => InternalServerError("File not found")
    }
  }

  def submitDatasetExtraction(ds_id: UUID) = PermissionAction(Permission.EditDataset, Some(ResourceRef(ResourceRef.dataset, ds_id))) { implicit request =>
    implicit val user = request.user
    val all_extractors = extractorService.listExtractorsInfo(List("EXTRACT", "CONVERT"))
    val extractors = all_extractors.filter(!_.process.dataset.isEmpty)
    datasetService.get(ds_id) match {
      case Some(ds) => Ok(views.html.extractions.submitDatasetExtraction(extractors, ds))
      case None => InternalServerError("Dataset not found")
    }
  }
}