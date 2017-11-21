package models

import play.api.libs.json.{Format, Json}

/**
  * Ресурс
  *
  * @param id          Идентифкатор
  * @param name        Наименование
  * @param description Описание
  */
case class Limit(id: Option[Long] = None, name: String, description: String)

/**
  * объект-компаньон
  */
object Limit {

  /**
    * @return Json view
    */
  implicit def format: Format[Limit] = Json.format[Limit]
}
