package controllers.common

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Интерфейс со вспомогательными методами
  * для возврата ответов в формате json
  * должен комбинироваться с [[Controller]]
  */
trait JsonHelper {
  self: Controller =>

  /**
    * Константа объект не найден в формате json
    */
  val ItemNotFound = NotFound(Json.obj("status" -> NOT_FOUND, "exception" -> "Item not found"))

  /**
    * Константа успешное выполнение в формате json
    */
  val Success = Ok(Json.obj("result" -> "Success"))

  /**
    * @param o   объект
    * @param tjs implicit writer
    * @tparam T type
    * @return result
    */
  def jsonOk[T](o: T)(implicit tjs: Writes[T]): Result = Ok(Json.toJson(o))

  /**
    * @param errors ошибки в формате json
    * @return result
    */
  def invalidJson(errors: Seq[(JsPath, Seq[ValidationError])]): Result =
    BadRequest(
      Json.obj(
        "status" -> BAD_REQUEST,
        "exception" -> "Invalid JSON",
        "body" -> Json.obj(
          "message" -> JsError.toJson(errors))
      )
    )

  /**
    * @param message сообщение
    * @return json bad request
    */
  def badRequest(message: String): Result = BadRequest(Json.obj("status" -> BAD_REQUEST, "exception" -> message))

  /**
    * @param r result
    * @return future
    */
  implicit def resultToFuture(r: Result): Future[Result] = Future.successful(r)

  /**
    * Запрещено
    *
    * @return Result
    */
  def forbidden(): Result = Forbidden(Json.obj("status" -> FORBIDDEN, "exception" -> "forbidden"))

}
