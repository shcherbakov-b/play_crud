package security

import play.api.mvc.Results.Unauthorized
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.{RequestHeader, Result}
import security.User.Role
import pdi.jwt.JwtSession._


/**
  * Аутентификации пользователя. Ожидает наличие JWT токена в запросе
  */
trait Authenticated {

  private val UsernameKey = "username"
  private val RolesKey = "roles"

  /**
    * получаем пользователя из JWT токена
    *
    * @param request оригинальный запрос
    * @return пользователь или None
    */
  def getUserFromRequest(request: RequestHeader): Option[User] = {
    request.jwtSession.getAs[String](UsernameKey).map { name =>
      val roles = request.jwtSession.getAs[Seq[Role]](RolesKey).getOrElse(Seq.empty[Role])
      User(name, roles)
    }
  }

  /**
    * Результат для неавторизованных запросов
    *
    * @param request запрос
    * @return 401 Unauthorized
    */
  def onUnauthorized(request: RequestHeader): Result = Unauthorized

  /**
    * Action для использования в контроллерах
    *
    * {{{
    * //  пример
    * def index = UserAction { implicit request =>
    *   Ok("Hello " + request.user)
    * }
    * }}}
    */
  object UserAction extends AuthenticatedBuilder(req => getUserFromRequest(req), req => onUnauthorized(req))

}
