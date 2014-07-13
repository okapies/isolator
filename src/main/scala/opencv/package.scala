import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.{Highgui, VideoCapture}

package object opencv {

  import ExecutionContext.Implicits.global

  def using[A <: {def release(): Unit}, B]
      (m: A)(f: A => B): B = {
    try {
      f(m)
    } finally {
      m.release()
    }
  }

  def using[A](a: (Mat, Int), b: (Mat, Int))(f: ((Mat, Int), (Mat, Int)) => A): A = {
    try {
      f(a, b)
    } finally {
      a._1.release()
      b._1.release()
    }
  }

  def add(a: Mat, b: Mat): Mat = {
    val dst = new Mat
    Core.add(a, b, dst)

    dst
  }

  def divide(a: Mat, b: Scalar): Mat = {
    val dst = new Mat
    Core.divide(a, b, dst)

    dst
  }

  def convertTo(rtype: Int)(a: Mat): Mat = {
    val dst = new Mat
    a.convertTo(dst, rtype)
    a.release() // TODO

    dst
  }

  def mean(size: Size)(typ: Int)(frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    val zero = Mat.zeros(size, imdType) -> 0
    def plus(a: (Mat, Int), b: (Mat, Int)) = add(a._1, b._1) -> (a._2 + b._2)
    val (sum, cnt) =
      frames
        .map(convertTo(imdType)(_) -> 1)
        .fold(zero)(using(_, _)(plus))

    using (sum) { sum =>
      divide(sum, new Scalar(cnt, cnt, cnt))
    }
  }

  def meanPar(batchSize: Int)
             (size: Size)
             (typ: Int)
             (frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    val zero = Mat.zeros(size, imdType) -> 0
    def plus(a: (Mat, Int), b: (Mat, Int)) = add(a._1, b._1) -> (a._2 + b._2)

    val partitions = frames.grouped(batchSize)
    val (sum, cnt) =
      partitions.map { ms =>
        ms.par.map(convertTo(imdType)(_) -> 1).fold(zero) { (a, b) =>
          val dst = plus(a, b)
          if (a._1 ne zero._1) { a._1.release() }
          if (b._1 ne zero._1) { b._1.release() }

          dst
        }
      }.fold(zero) { (a, b) =>
        val dst = plus(a, b)
        if (a._1 ne zero._1) { a._1.release() }
        if (b._1 ne zero._1) { b._1.release() }

        dst
      }
/*
      Await.result(
        Future.traverse(partitions) { ms: Iterator[Mat] =>
          Future(ms.map(convertTo(imdType)(_) -> 1).fold(zero)(plus))
        }.map { ps: Iterator[(Mat, Int)] =>
          ps.fold(zero)(plus)
        },
        Duration.Inf)
*/

    using (sum) { sum =>
      divide(sum, new Scalar(cnt, cnt, cnt))
    }
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
