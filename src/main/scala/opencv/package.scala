import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.{Highgui, VideoCapture}

package object opencv {

  import ExecutionContext.Implicits.global

  def add(a: Mat, b: Mat): Mat = {
    val dst = new Mat
    Core.add(a, b, dst)
    a.release()
    b.release()

    dst
  }

  def divide(a: Mat, b: Scalar): Mat = {
    val dst = new Mat
    Core.divide(a, b, dst)
    a.release()

    dst
  }

  def convertTo(rtype: Int)(a: Mat): Mat = {
    val dst = new Mat
    a.convertTo(dst, rtype)
    a.release()

    dst
  }

  def mean(size: Size)(typ: Int)(frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    def zeros = Mat.zeros(size, imdType)
    val (sum, cnt) =
      frames
        .map(convertTo(imdType))
        .foldLeft((zeros, 0)) { case ((sum, i), m) => (add(sum, m), i + 1) }

    divide(sum, new Scalar(cnt, cnt, cnt))
  }

  def meanPar(batchSize: Int)(size: Size)(typ: Int)(frames: Iterator[Mat]): Mat = {
    val imdType = CvType.CV_32SC3 // TODO

    def zero = Mat.zeros(size, imdType) -> 0
    def plus(a: (Mat, Int), b: (Mat, Int)) = add(a._1, b._1) -> (a._2 + b._2)

    val partitions =
      Iterator.continually(frames.take(batchSize).toSeq).takeWhile(_.nonEmpty)
    val (sum, cnt) =
      Await.result(
        Future.traverse(partitions) { ms: Seq[Mat] =>
          Future(ms.map(convertTo(imdType)(_) -> 1).fold(zero)(plus))
        }.map { ps: Iterator[(Mat, Int)] =>
          ps.fold(zero)(plus)
        },
        Duration.Inf)

    divide(sum, new Scalar(cnt, cnt, cnt))
  }

  def loadVideo[A](filename: String)(f: Size => Int => Iterator[Mat] => A): A = {
    val cap = new VideoCapture(filename)
    try {
      if (cap.grab()) {
        val head = new Mat
        cap.retrieve(head)
        val size = head.size
        val typ = head.`type`

        f(size)(typ)(
          Iterator.single(head)
            ++ Iterator.continually(nextFrame(cap)).takeWhile(_ != None).map(_.get))
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
