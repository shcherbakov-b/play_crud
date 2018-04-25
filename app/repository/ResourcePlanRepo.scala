package repository

import javax.inject.Inject

import models.{Limit, PlanRole, ResourcePlan}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.SnowflakeGenerator
import slick.driver.JdbcProfile
import utils.QueryParameters.QueryParams

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Хранинение ресурсных планов
  *
  * @param dbConfigProvider конфиг БД
  */
class ResourcePlanRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  /**
    * query к PlanLimits
    */
  private[this] val PlanLimits = TableQuery[PlanLimitTable]

  /**
    * query к Plans
    */
  private[this] val Plans = TableQuery[PlanTable]

  /**
    * query к Limits
    */
  private[this] val Limits = TableQuery[LimitTable]

  /**
    * query к PlanRoles
    */
  private[this] val Roles = TableQuery[PlanRoleTable]

  /**
    * ид генератор
    */
  private[this] val Snowflake = new SnowflakeGenerator

  /**
    * Поиск ограничения
    *
    * @param queryParams Параметры запроса(обязательно должен присутсвовать пользователь и фильтр)
    * @return Future
    */
  def findLimitPlan(queryParams: QueryParams): Future[Option[Int]] =
    queryParams.filter match {
      case Some(f) => queryParams.user match {
        case Some(u) if u.roles.nonEmpty => db.run {
          (for {
            l <- Limits if l.name === f.pattern //join
            pl <- PlanLimits if pl.limitId === l.id
            p <- Plans if p.id === pl.planId
            r <- Roles if r.planId === p.id
          } yield (l, pl, p, r))
            .filter(_._4.roleId inSetBind u.roles.map(_.id)) //filter
            .sortBy(_._2.value.desc).take(1).map(_._2.value).result.headOption //sort
        }
      }
      case _ => Future.successful(None)
    }

  /**
    * Запрос за всеми ресурсными планами
    *
    * @param queryParams Параметры запроса(пейджинация)
    * @return Future[Seq]
    */
  def all(queryParams: QueryParams): Future[Seq[ResourcePlan]] = {

    def paged(query: Query[PlanTable, Plan, Seq]) = {
      queryParams.page match {
        case None => query
        case Some(p) => query.drop(p.offset).take(p.limit)
      }
    }

    db.run((for {
      p <- paged(Plans)
      pl <- PlanLimits if pl.planId === p.id
      l <- Limits if l.id === pl.planId
      r <- Roles if r.planId === p.id
    } yield (p, pl, l, r)).result).map { seq =>
      val plans = seq.map(_._1)
      val planLimits = seq.map(_._2)
      val limits = seq.map(_._3)
      val roles = seq.map(_._4)
      plans.map { plan =>
        val mapped = planLimits.filter(_.planId == plan.id.get).flatMap(pl => limits.filter(_.id.get == pl.limitId)
          .map(l => (l, pl.value)))
        ResourcePlan(plan.id, plan.name, plan.description, plan.status, mapped, Some(roles.filter(_.planId == plan.id.get)))
      }
    }
  }

  /**
    * Создание всей цепочки планов
    *
    * @param resource Составная сущность
    * @return Future
    */
  def upsertPlan(resource: ResourcePlan): Future[ResourcePlan] = {
    def upsert = {
      val plan = Plan(if (resource.planId.isEmpty) Some(Snowflake.next) else resource.planId,
        resource.name, resource.description, resource.status)
      val limits = resource.limits.map {
        case (limit, value) => if (limit.id.isEmpty) (limit.copy(id = Some(Snowflake.next)), value) else (limit, value)
      }
      db.run {
        Plans.insertOrUpdate(plan) zip
          DBIO.sequence(limits.map {
            case (limit, _) => Limits.insertOrUpdate(limit)
          }).transactionally
      }.flatMap { _ =>
        db.run(DBIO.sequence(limits.map {
          case (limit, value) => PlanLimits.insertOrUpdate(PlanLimit(limit.id.get, plan.id.get, value))
        })).map { _ =>
          resource.copy(planId = plan.id, limits = limits)
        }
      }
    }

    resource.planId.map(id => removePlans(id).flatMap(_ => upsert)).getOrElse(upsert)
  }

  /**
    * Поиск по Ид
    *
    * @param planId Идентификатор плана
    * @return ResourcePlan либо None
    */
  def findById(planId: Long): Future[Option[ResourcePlan]] = {
    db.run {
      val plan = Plans.filter(_.id === planId)
      plan.result.headOption zip plan.flatMap(_.limits).result
    }.flatMap {
      case (plan, limits) => plan match {
        case None => Future.successful(None)
        case Some(p) => db.run(Roles.filter(_.planId === planId).result zip PlanLimits.filter(_.planId === planId).result)
          .map {
            case (roles, planLimits) =>
              val mapped = planLimits.filter(_.planId == p.id.get).flatMap(pl => limits.filter(_.id.get == pl.limitId)
                .map(l => (l, pl.value)))
              Some(models.ResourcePlan(p.id, p.name, p.description, p.status, mapped, Some(roles)))
          }
      }
    }
  }

  /**
    * Связываются роль и планы
    *
    * @param planIds Идентификаторы планов
    * @param roleId  ид проли
    * @return Row affected
    */
  def bindRoleWithPlan(planIds: Seq[Long], roleId: Long): Future[Seq[Int]] = {
    val toBind = planIds.map(p => Roles.insertOrUpdate(PlanRole(p, roleId)))
    db.run(DBIO.sequence(toBind).transactionally)
  }

  /**
    * Удаление всей цепочки планов
    *
    * @param planId Plan Id
    * @return 0 если объект не найден или другое если удален
    */
  def removePlans(planId: Long): Future[Int] = db.run {
    val plan = Plans.filter(_.id === planId)
    plan.flatMap(_.limits).delete zip
      plan.delete
  }.map(_._1)

  /**
    * Ресурный план для ролей
    *
    * @param limitId Ссылка на ресурс
    * @param planId  Ссылка на план
    * @param value   Значение
    */
  private[this] case class PlanLimit(limitId: Long, planId: Long, value: Int)

  /**
    * dao
    *
    * @param tag тэг
    */
  private[this] class PlanLimitTable(tag: Tag) extends Table[PlanLimit](tag, "plan_limits") {

    /**
      * @return role
      */
    def planId: Rep[Long] = column[Long]("plan_id")

    /**
      * @return restriction id
      */
    def limitId: Rep[Long] = column[Long]("limit_id")

    /**
      * @return value
      */
    def value: Rep[Int] = column[Int]("value")

    /**
      * @return pk
      */
    def pk = primaryKey("plan_limits_pk", (planId, limitId))

    /**
      * @return plans
      */
    def plans = foreignKey("plans", planId, Plans)(plan => plan.id, onDelete = ForeignKeyAction.Cascade)

    /**
      * @return limits
      */
    def limits = foreignKey("limits", limitId, Limits)(limit => limit.id, onDelete = ForeignKeyAction.Cascade)

    def * = (planId, limitId, value) <> ((PlanLimit.apply _).tupled, PlanLimit.unapply)

  }

  /**
    * dao
    *
    * @param tag Tag
    */
  private[this] class PlanRoleTable(tag: Tag) extends Table[PlanRole](tag, "plan_roles") {

    /**
      * @return role
      */
    def planId: Rep[Long] = column[Long]("plan_id")

    /**
      * @return roleId
      */
    def roleId: Rep[Long] = column[Long]("role_id")

    /**
      * @return pk
      */
    def pk = primaryKey("plan_roles_pk", (planId, roleId))

    /**
      * @return plans
      */
    def plans = foreignKey("plans", planId, Plans)(plan => plan.id, onDelete = ForeignKeyAction.Cascade)

    def * = (planId, roleId) <> ((PlanRole.apply _).tupled, PlanRole.unapply)

  }

  /**
    * Представление плана
    *
    * @param id          Ид плана
    * @param name        Название
    * @param description Описание
    * @param status      Статус
    */
  private[this] case class Plan(id: Option[Long] = None, name: String, description: String, status: Boolean)

  /**
    * dao
    *
    * @param tag тэг
    */
  private[this] class PlanTable(tag: Tag) extends Table[Plan](tag, "plans") {

    /**
      * @return id
      */
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey)

    /**
      * @return name
      */
    def name: Rep[String] = column[String]("name")

    /**
      * @return description
      */
    def description: Rep[String] = column[String]("description")

    /**
      * @return status
      */
    def status: Rep[Boolean] = column[Boolean]("status")

    def * = (id.?, name, description, status) <> ((Plan.apply _).tupled, Plan.unapply)

    /**
      * @return Plans
      */
    def limits = PlanLimits.filter(_.planId === id).flatMap(_.limits)

  }

  /**
    * dao
    *
    * @param tag тэг
    */
  private[this] class LimitTable(tag: Tag) extends Table[Limit](tag, "limits") {

    /**
      * @return id
      */
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey)

    /**
      * @return name
      */
    def name: Rep[String] = column[String]("name")

    /**
      * @return description
      */
    def description: Rep[String] = column[String]("description")

    def * = (id.?, name, description) <> ((Limit.apply _).tupled, Limit.unapply)

    /**
      * @return Plans
      */
    def plans = PlanLimits.filter(_.limitId === id).flatMap(_.plans)

  }

}
