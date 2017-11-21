package security

import play.api.libs.json.{Format, Json}
import security.User.{AdminRole, Role}

/**
  * Пользователь
  *
  * @param name  имя
  * @param roles роли
  */
case class User(name: String, roles: Seq[Role] = Seq.empty[Role]) {

  /**
    * Админ ли пользователь
    *
    * @return true/false
    */
  def hasAdminRole: Boolean = roles.exists(r => r.name.fold(false)(_ == AdminRole))
}

/** компаньон */
object User {

  /** роль админа */
  val AdminRole = "admin"

  /**
    * Роль
    *
    * @param id   Идентификатор
    * @param name имя
    */
  case class Role(id: Long, name: Option[String] = None)

  /** компаньон */
  object Role {

    /** формат */
    implicit val format: Format[Role] = Json.format[Role]
  }

}
