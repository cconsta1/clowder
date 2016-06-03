package models

import java.util.Date
import play.api.libs.json.{Json, JsValue, Writes}
import securesocial.core.Identity

/**
  * Created by todd_n on 2/8/16.
  */
case class VocabularyTerm(
  id : UUID = UUID.generate(),
  author : Option[Identity],
  created : Date = new Date(),
  key : String,
  units : Option[String],
  default_value : Option[String]
  )


object VocabularyTerm{
  implicit val vocabularyTermWrites = new Writes[VocabularyTerm] {
    def writes(vocabularyTerm : VocabularyTerm) : JsValue = {
      val default = if (vocabularyTerm.default_value.isEmpty){
        ""
      } else {
        vocabularyTerm.default_value.get
      }
      Json.obj("id" -> vocabularyTerm.id.toString,"key"->vocabularyTerm.key,"default_value"->default)
    }
  }
}



