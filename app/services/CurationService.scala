package services

import java.net.URI

import models._


/**
 * Service to manipulate curation objects.
 */
trait CurationService {
  /**
   * insert a new curation object.
   */
  def insert(curation: CurationObject)

  /**
   * Get curation object.
   */
  def get(id: UUID): Option[CurationObject]

  /**
   * Update status of a curation object.
   */
  def updateStatus(id: UUID, status: String)

  /**
   * Set the curation object as submitted and set the submitted date to current date
   */
  def setSubmitted(id: UUID)

  /**
   * Set the curation object as Published and set the publish date to current date.
   */
  def setPublished(id:UUID)

  /**
   * remove curation object, also delete it from staging area.
   */
  def remove(id: UUID): Unit

  /**
   * insert a new curation object file.
   */
  def insertFile(curationFile : CurationFile)

  /**
   * insert a new curation object folder.
   */
  def insertFolder(curationFolder : CurationFolder)

  /**
   * Update the repository selected
   */
  def updateRepository(curationId: UUID, repository: String)

  /**
   * Save external Identifier received from repository
   */
  def updateExternalIdentifier(curationId: UUID, externalIdentifier: URI)

  /**
   * List curation and published objects a dataset is related to.
   */
  def getCurationObjectByDatasetId(datasetId: UUID): List[CurationObject]


  /**
   * List curation files of a curation obeject
   */
  def getCurationFiles(curationFileIds:List[UUID]): List[CurationFile]

  /**
   * List curation folders of a curation obeject
   */
  def getCurationFolders(curationFolderIds:List[UUID]): List[CurationFolder]

  /**
   * get the curation contains this curation file
   */
  def getCurationByCurationFile(curationFile: UUID): Option[CurationObject]

  /**
   * get the curation folder
   */
  def getCurationFolder(curationFolderId: UUID): Option[CurationFolder]

  /**
   * Delete a curation file from a curation obeject
   */
  def deleteCurationFiles(curationId: UUID, curationFileId: UUID)

  /**
   * Update curation object's name, description, space.
   */
  def updateInformation(id: UUID, description: String, name: String, oldSpace: UUID, newSpace:UUID)
}
