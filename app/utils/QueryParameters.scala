package utils

import security.User
import slick.ast.Ordering.{Asc, Desc, Direction}

/**
  * Объект содержащий различные фильтры запроса
  */
object QueryParameters {

  /**
    * параметры запроса
    * @param page страница
    * @param filter фильтры
    * @param sort сортирвка
    * @param user пользователь
    * @param public доступность
    */
  case class QueryParams(
                          page: Option[Page] = None,
                          filter: Option[Filter] = None,
                          sort: Option[Sort] = None,
                          user : Option[User] = None,
                        )

  /** запрос без параметров*/
  val Empty = QueryParams()

  /**
    * Пейджинация
    *
    * @param number номер страницы
    * @param size   размер
    */
  case class Page(private val number: Int, private val size: Option[Int]) {

    /** количество элементов */
    lazy val limit: Int = size.fold(Int.MaxValue)(s => s)

    /** оффсет */
    lazy val offset: Int = size.fold(0)(s => if (number <= 0) 0 else (number - 1) * s)

  }

  /** объект компаньон */
  object Page {

    /**
      * Инициализация страницы
      *
      * @param number номер
      * @param size   размер
      * @return Some or None
      */
    def of(number: Option[Int], size: Option[Int]): Option[Page] = {
      number.flatMap(n => size.map(s => new Page(n, Some(s)))).orElse(None)
    }
  }

  /**
    * Фильтрация
    *
    * @param field   поле
    * @param pattern паттерн
    */
  case class Filter(field: String, pattern: String)

  /** объект компаньон */
  object Filter {

    /**
      * Инициализация фильтра
      *
      * @param field   поле по которому будет выполняться фильтрация
      * @param pattern шаблон поиска
      * @return Option
      */
    def of(field: Option[String], pattern: Option[String]): Option[Filter] = {
      field.flatMap(f => pattern.map(p => new Filter(f, p))).orElse(None)
    }
  }

  /**
    * Сортировка
    *
    * @param field поле по которому выполнится сортировка
    * @param direction направление
    */
  case class Sort(field: String, direction: Direction)

  /**
    * объект компаньон
    */
  object Sort {

    /**
      * Инициализация сортировки
      *
      * @param field   поле по которому будет выполняться сортировка
      * @param direction направление
      * @return Option
      */
    def of(field: Option[String], direction: Option[String]): Option[Sort] = {
      field.flatMap(f => direction.map {
        case "ASC" => new Sort(f, Asc)
        case "DESC" => new Sort(f, Desc)
      }).orElse(None)
    }
  }
}
