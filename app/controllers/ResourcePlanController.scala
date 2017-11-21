package controllers

import javax.inject.Inject

import controllers.common.JsonHelper
import models.ResourcePlan
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Controller}
import repository.ResourcePlanRepo
import security.Authenticated
import utils.QueryParameters.{Filter, Page, QueryParams}

import scala.concurrent.Future

/**
  * Досутп к ресурсным планам
  *
  * @param repo репозиторий для планов
  */
class ResourcePlanController @Inject()(repo: ResourcePlanRepo) extends Controller with JsonHelper with Authenticated {

  /**
    * @param number номер страницы
    * @param size   размер
    * @return Json array
    */
  def list(number: Option[Int], size: Option[Int]): Action[AnyContent] = UserAction.async {
    repo.all(QueryParams(page = Page.of(number, size))).map(jsonOk(_))
  }

  /**
    * @param id Идентификатор
    * @return NotFound or json item
    */
  def get(id: Long): Action[AnyContent] = UserAction.async {
    repo.findById(id).map(_.fold(ItemNotFound)(jsonOk(_)))
  }

  /**
    * @return Созданый объект в json
    */
  def upsert(): Action[JsValue] = UserAction.async(parse.json) { request =>
    request.body.validate[ResourcePlan].fold(errors => Future.successful(invalidJson(errors)), { input =>
      repo.upsertPlan(input).map(created => Created(toJson(created)))
    })
  }

  /**
    * @param id Идентификатор
    * @return Success or NotFound
    */
  def delete(id: Long): Action[AnyContent] = UserAction.async {
    repo.removePlans(id).map(i => if (i > 0) Success else ItemNotFound)
  }

  /**
    * Поиск ограничения для пользователя
    *
    * @param service Название сервиса(api_gateway/cassandra)
    * @return Success or NotFound
    */
  def limits(service: String): Action[AnyContent] = UserAction.async { request =>
    repo.findLimitPlan(QueryParams(user = Some(request.user), filter = Some(Filter("name", service))))
      .map(_.fold(ItemNotFound)(value => Ok(Json.obj("limits" -> value))))
  }

  /**
    * Связывваем роли с текущими планами
    *
    * @param roleId  Ид роли
    * @param planIds Идентификаторы планов
    * @return Success or NotFound
    */
  def bindRoles(roleId: Long, planIds: Seq[Long]): Action[AnyContent] = UserAction.async {
    repo.bindRoleWithPlan(planIds, roleId).map(seq => if (seq.nonEmpty) Success else ItemNotFound)
  }

}
