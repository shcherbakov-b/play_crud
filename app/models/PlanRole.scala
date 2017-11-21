package models

import play.api.libs.json.{Format, Json}

/**
  * Отношение план - роль
  *
  * @param planId Идентификатор плана
  * @param roleId Идентификатор роли пользователя
  */
case class PlanRole(planId: Long, roleId: Long)

/**
  * объект-компаньон
  */
object PlanRole {

  /**
    * @return Json view
    */
  implicit def format: Format[PlanRole] = Json.format[PlanRole]
}