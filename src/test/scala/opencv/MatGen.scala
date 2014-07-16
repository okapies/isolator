package opencv

import scala.math.abs

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.util.Pretty

import org.opencv.core.{CvType, Mat}

object MatGen {

  import scala.language.implicitConversions
  import Arbitrary._
  import Gen._

  implicit def prettyMat(m: Mat): Pretty = prettyMatWithDump(m)

  def prettyArrayMat(ms: Array[Mat]): Pretty = Pretty { params =>
    ms.map(prettyMatWithDump(_)(params)).mkString("[", ", ", "]")
  }

  def prettyTraversableMat(ms: Traversable[Mat]): Pretty = Pretty { params =>
    ms.map(prettyMatWithDump(_)(params)).mkString("[", ", ", "]")
  }

  private[this] def prettyMatWithDump(m: Mat): Pretty =
    Pretty(_ => "Mat(" +
      s"size=${m.size}, " +
      s"type=${CvType.typeToString(m.`type`)}, " +
      s"isContinuous=${m.isContinuous}, " +
      s"isSubmatrix=${m.isSubmatrix}, " +
      s"nativeObj=0x${m.nativeObj.toHexString}, " +
      s"dataAddr=0x${m.dataAddr.toHexString}, " +
      s"data=${m.dump.filter(_ != '\n')})")

  private[this] def matIntImpl(rows: Int, cols: Int, ch: Int, elem: Gen[Int]): Gen[Mat] =
    for {
      data <- containerOfN[Array, Int](rows * cols * ch, elem)
    } yield {
      val m = new Mat(rows, cols, CvType.CV_32SC(ch))
      m.put(0, 0, data)

      m
    }

  def matInt(rows: Gen[Int],
             cols: Gen[Int],
             ch: Gen[Int] = choose(1, 4),
             elem: Gen[Int] = arbitrary[Int]) =
    for {
      rows <- rows
      cols <- cols
      ch <- ch
      m <- matIntImpl(rows, cols, ch, elem)
    } yield m

  def matInt2(rows: Gen[Int],
              cols: Gen[Int],
              ch: Gen[Int] = choose(1, 4),
              elem: Gen[Int] = arbitrary[Int]) =
    for {
      rows <- rows
      cols <- cols
      ch <- ch
      m2 <- zip(
        matIntImpl(rows, cols, ch, elem),
        matIntImpl(rows, cols, ch, elem))
    } yield m2

  def matInt3(rows: Gen[Int],
              cols: Gen[Int],
              ch: Gen[Int] = choose(1, 4),
              elem: Gen[Int] = arbitrary[Int]) =
    for {
      rows <- rows
      cols <- cols
      ch <- ch
      m3 <- zip(
        matIntImpl(rows, cols, ch, elem),
        matIntImpl(rows, cols, ch, elem),
        matIntImpl(rows, cols, ch, elem))
    } yield m3

  def matIntList(rows: Gen[Int],
                 cols: Gen[Int],
                 length: Gen[Int],
                 ch: Gen[Int] = choose(1, 4),
                 elem: Gen[Int] = arbitrary[Int]) =
    for {
      rows <- rows
      cols <- cols
      length <- length
      ch <- ch
      ms <- Gen.containerOfN[List, Mat](
        length,
        matIntImpl(rows, cols, ch, elem.retryUntil(abs(_) < (Int.MaxValue / length))))
    } yield ms

}
