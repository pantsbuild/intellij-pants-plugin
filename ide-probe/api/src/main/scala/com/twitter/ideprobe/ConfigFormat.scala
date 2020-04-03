package com.twitter.ideprobe

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.typesafe.config.ConfigValueFactory
import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.error.CannotConvert
import pureconfig.error.ConfigReaderFailures
import pureconfig.error.ConvertFailure
import pureconfig.generic.ProductHint
import scala.reflect.ClassTag
import scala.util.Try

// This trait should always be mixed-in to object where we create ConfigReaders
// so that we use consistent ProductHint that is responsible for prohibiting
// unknown keys and case class name mapping (to keep camelCase, default is kebab-case)
trait ConfigFormat {

  private val dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  implicit val localDateTimeReader: ConfigReader[LocalDateTime] =
    ConfigReader.fromNonEmptyStringTry(s => Try(LocalDateTime.parse(s, dateTimeFormat)))
  implicit val localDateTimeWriter: ConfigWriter[LocalDateTime] =
    ConfigWriter.fromFunction(l => ConfigValueFactory.fromAnyRef(l.format(dateTimeFormat)))

  implicit val unitReader: ConfigReader[Unit] = ConfigReader
    .fromCursor(c =>
      if (c.isNull) Right(())
      else Left(ConfigReaderFailures(ConvertFailure(CannotConvert(c.toString, "Unit", "Expected null"), c)))
    )
  implicit val unitWriter: ConfigWriter[Unit] = ConfigWriter.fromFunction(_ => ConfigValueFactory.fromAnyRef(null))

  // Default derivation of ADT adds `type` field, which would require us to specify
  // the type each time e.g. { type = Direct, id = "org.intellij.scala", version = "2019.3.1" }
  // or { type = filesystem, path = "/tmp" }. It is more convenient for users, especially for such
  // common classes if we can differentiate what class it is by set of fields. We just have to be sure
  // to not introduce e.g. 2 classes with single field 'path'.
  def possiblyAmbiguousAdtReader[Base](readers: ConfigReader[_]*)(implicit ct: ClassTag[Base]): ConfigReader[Base] =
    (cur: ConfigCursor) => {
      val read = readers.map(_.from(cur)).asInstanceOf[Seq[Result[Base]]]

      read.reduceLeft(_.orElse(_)).left.map { _ =>
        def errors[A](res: Result[A]): List[ConfigReaderFailures] = {
          res.left.toSeq.toList
        }
        val cls = ct.runtimeClass.getName
        val header = ConvertFailure(CannotConvert("", cls, "None of the below alternatives matched"), cur)
        read.flatMap(errors).foldLeft(ConfigReaderFailures(header))(_ ++ _)
      }
    }

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), useDefaultArgs = true, allowUnknownKeys = false)
}
