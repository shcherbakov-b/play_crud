package services

/**
  * Генератор числовых идентификаторов с использованием алгоритма Snowflake
  */
object SnowflakeGenerator {
  /** Смещение для идентификатора ноды */
  val NodeShift = 3
  /** Смещение для счетчика */
  val SeqShift = 10
  /** Максимальное значение ноды */
  val MaxNode = 8
  /** Максимальное значение счетчика */
  val MaxSequence = 1024
}

/**
  * Сервис для генерации последовательностей
  *
  * @param node Номер ноды в кластере
  */
class SnowflakeGenerator(val node: Int = 1) {

  import SnowflakeGenerator._

  require(node >= 0 && node < MaxNode, s"node must be between 0 and $MaxNode")
  private var sequence = 0
  private var referenceTime = 0L

  /**
    * Генерация следующего идентификатора
    *
    * @return Long Идентификатор
    */
  def next: Long = {
    val currentTime = System.currentTimeMillis
    var counter = 0L
    this.synchronized {
      if (currentTime < referenceTime) {
        throw new RuntimeException("Last referenceTime " + referenceTime + " is after reference time " + currentTime)
      }
      else if (currentTime > referenceTime) {
        this.sequence = 0
      }
      else if (this.sequence < MaxSequence) {
        this.sequence += 1
      }
      else {
        throw new RuntimeException("Sequence exhausted at " + this.sequence)
      }
      counter = this.sequence
      referenceTime = currentTime

      currentTime << NodeShift << SeqShift | node << SeqShift | counter
    }
  }
}