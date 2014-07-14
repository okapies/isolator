import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.{Highgui, VideoCapture}

package object opencv {

  import ExecutionContext.Implicits.global

  def managed[A](a: Mat)(f: Mat => A): A = {
    try {
      f(a)
    } finally {
      a.release()
    }
  }

  def managed[A](a: Mat, b: Mat)(f: Mat => Mat => A): A = {
    try {
      f(a)(b)
    } finally {
      a.release()
      b.release()
    }
  }

  def add(a: Mat)(b: Mat): Mat = {
    val dst = new Mat
    Core.add(a, b, dst)

    dst
  }

  def divide(a: Mat)(b: Scalar): Mat = {
    val dst = new Mat
    Core.divide(a, b, dst)

    dst
  }

  def convertTo(rtype: Int)(a: Mat): Mat = {
    val dst = new Mat
    a.convertTo(dst, rtype)

    dst
  }

  def mean(size: Size)(typ: Int)(frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    def widen(m: Mat) = managed(m)(convertTo(imdType))
    val zero = Mat.zeros(size, imdType) -> 0
    def plus(a: (Mat, Int), b: (Mat, Int)) = managed(a._1, b._1)(add) -> (a._2 + b._2)

    val (sum, cnt) = frames.map(widen(_) -> 1).fold(zero)(plus)

    managed(sum)(m => divide(m)(new Scalar(cnt, cnt, cnt)))
  }

  def meanPar(batchSize: Int)(size: Size)(typ: Int)(frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    def widen(m: Mat) = managed(m)(convertTo(imdType))
    def plus(a: (Mat, Int), b: (Mat, Int)) = managed(a._1, b._1)(add) -> (a._2 + b._2)

    val partitions = frames.grouped(batchSize)
    val (sum, cnt) =
      partitions
        .map(_.par.map(widen(_) -> 1).reduce(plus))
        .reduce(plus)
/*
    val (sum, cnt) = Await.result(
      Future.traverse(partitions) { ms: Iterator[Mat] =>
        Future(ms.map(widen(_) -> 1).reduce(plus))
      }.map { ps: Iterator[(Mat, Int)] =>
        ps.reduce(plus)
      },
      Duration.Inf)
*/

    managed(sum)(divide(_)(new Scalar(cnt, cnt, cnt)))
  }

  def loadVideo[A, B](filename: String)
                     (f: Size => Int => Iterator[Mat] => A): A = {
    val cap = new VideoCapture(filename)
    try {
      if (cap.grab()) {
        val head = new Mat
        cap.retrieve(head)
        val size = head.size
        val typ = head.`type`

        f(size)(typ)(
          Iterator.single(head) ++
          Iterator.continually(nextFrame(cap)).takeWhile(_ != None).map(_.get))
      } else {
        f(new Size)(-1)(Iterator.empty)
      } 
    } finally {
      cap.release()
    }
  }

  def nextFrame(cap: VideoCapture): Option[Mat] = {
    val frame = new Mat
    if (cap.read(frame)) {
      Some(frame)
    } else {
      None
    }
  }

  def saveImage(filename: String, m: Mat) = Highgui.imwrite(filename, m)

}
