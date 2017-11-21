package models

import play.api.data.validation.ValidationError
import play.api.libs.json._

/**
  * Ресурсный план
  *
  * @param planId      Ид плана
  * @param name        Название плана
  * @param description Описание плана
  * @param status      Статус плана
  * @param limits      ограничения
  * @param roles       роли
  */
case class ResourcePlan(planId: Option[Long] = None,
                        name: String,
                        description: String,
                        status: Boolean,
                        limits: Seq[(Limit, Int)] = Seq.empty[(Limit, Int)],
                        roles: Option[Seq[PlanRole]] = None
                       )

/** компаньон */
object ResourcePlan {

  /**
    * Writer for tuple
    *
    * @param a Limit
    * @param b Int
    * @return writer
    */
  implicit def tuple2Writes(implicit a: Writes[Limit], b: Writes[Int]): Writes[(Limit, Int)] = Writes[(Limit, Int)] {
    tuple: (Limit, Int) => JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
  }

  /**
    * Reader for tuple
    *
    * @param aReads Limit
    * @param bReads Int
    * @return reader
    */
  implicit def tuple2Reads(implicit aReads: Reads[Limit], bReads: Reads[Int]): Reads[(Limit, Int)] = Reads[(Limit, Int)] {
    case JsArray(arr) if arr.size == 2 => for {
      a <- aReads.reads(arr.head)
      b <- bReads.reads(arr.tail.head)
    } yield (a, b)
    case _ => JsError(Seq(JsPath() -> Seq(ValidationError("Expected array of two elements"))))
  }

  /** формат */
  implicit val format: Format[ResourcePlan] = Json.format[ResourcePlan]
}



