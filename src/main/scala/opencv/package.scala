import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.VideoCapture

package object opencv {

  def using[A](a: Mat)(f: Mat => A): A = {
    try {
      f(a)
    } finally {
      a.release()
    }
  }

  def using[A](a: Mat, b: Mat)(f: (Mat, Mat) => A): A = {
    try {
      f(a, b)
    } finally {
      a.release()
      b.release()
    }
  }

  def using[A](a: Mat, b: Mat, c: Mat)(f: (Mat, Mat, Mat) => A): A = {
    try {
      f(a, b, c)
    } finally {
      a.release()
      b.release()
      c.release()
    }
  }

  def using[A](cap: VideoCapture)(f: VideoCapture => A): A = {
    try {
      f(cap)
    } finally {
      cap.release()
    }
  }

  def zeros(rows: Int, cols: Int, typ: Int) = Mat.zeros(rows, cols, typ)

  def zeros(size: Size, typ: Int) = Mat.zeros(size, typ)

  implicit class RichMat(val self: Mat) extends AnyVal {

    def consume[A](f: Mat => A): A = using(self)(f)

    def +(other: Mat): Mat = {
      val dst = new Mat
      Core.add(self, other, dst)

      dst
    }

    def /(other: Scalar): Mat = {
      val dst = new Mat
      Core.divide(self, other, dst)

      dst
    }

    def compare(other: Mat, cmpop: Int): Mat = {
      val dst = new Mat
      Core.compare(self, other, dst, cmpop)

      dst
    }

    def convertTo(rtype: Int): Mat = {
      val dst = new Mat
      self.convertTo(dst, rtype)

      dst
    }

    def copy(): Mat = {
      val dst = new Mat
      self.copyTo(dst)

      dst
    }

    def copy(mask: Mat): Mat = {
      val dst = new Mat
      self.copyTo(dst, mask)

      dst
    }

    def countNonZero: Int = Core.countNonZero(self)

    def eq(other: Mat): Boolean = other match {
      case _ if self.empty() && other.empty() => true
      case _ if
        self.rows != other.rows ||
        self.cols != other.cols ||
        self.`type` != other.`type` ||
        self.dims != other.dims => false
      case _ =>
        self.compare(other, Core.CMP_NE).consume { diff =>
          diff.extractChannels().forall(_.consume(_.countNonZero == 0))
        }
    }

    def extractChannel(coi: Int): Mat = {
      val dst = new Mat
      Core.extractChannel(self, dst, coi)

      dst
    }

    def extractChannels(): Iterator[Mat] =
      (0 until self.channels).toIterator.map(self.extractChannel)

  }

  implicit class RichMatTuple2(val self: (Mat, Mat)) extends AnyVal {
    def consume[A](f: (Mat, Mat) => A): A = using(self._1, self._2)(f)
  }

  implicit class RichMatTuple3(val self: (Mat, Mat, Mat)) extends AnyVal {
    def consume[A](f: (Mat, Mat, Mat) => A): A = using(self._1, self._2, self._3)(f)
  }

  implicit class VideoCaptureIterator(val cap: VideoCapture) extends Iterator[Option[Mat]] {

    override def hasNext: Boolean = true // always produce Some[Mat] or None

    override def next(): Option[Mat] =
      if (cap.grab()) {
        val image = new Mat
        cap.retrieve(image)

        Some(image)
      } else {
        None
      }

  }

  def mean(frames: Iterator[Mat]): Mat = {
    if (frames.hasNext) {
      val head = frames.next()
      val size = head.size
      val imdType = CvType.CV_32SC3 // TODO

      def widen(m: Mat) = m.consume(_.convertTo(imdType))
      val zero = zeros(size, imdType) -> 0
      def plus(a: (Mat, Int), b: (Mat, Int)) = (a._1, b._1).consume(_ + _) -> (a._2 + b._2)

      val (sum, cnt) = (Iterator.single(head) ++ frames).map(widen(_) -> 1).fold(zero)(plus)

      sum.consume(_ / new Scalar(cnt, cnt, cnt))
    } else {
      zeros(0, 0, CvType.CV_32SC3)
    }
  }

  def meanPar(frames: Iterator[Mat], batchSize: Int): Mat = {
    if (frames.hasNext) {
      val head = frames.next()
      val size = head.size
      val imdType = CvType.CV_32SC3 // TODO

      def widen(m: Mat) = m.consume(_.convertTo(imdType))
      val zero = zeros(size, imdType) -> 0
      def plus(a: (Mat, Int), b: (Mat, Int)) = (a._1, b._1).consume(_ + _) -> (a._2 + b._2)

      val partitions = (Iterator.single(head) ++ frames).grouped(batchSize)
      val (sum, cnt) =
        partitions
          .map(_.par.map(widen(_) -> 1).fold(zero)(plus))
          .fold(zero)(plus)

/*
      import scala.concurrent.{Await, ExecutionContext, Future}
      import scala.concurrent.duration.Duration
      import ExecutionContext.Implicits.global

      val (sum, cnt) = Await.result(
        Future.traverse(partitions) { ms: Iterator[Mat] =>
          Future(ms.map(widen(_) -> 1).reduce(plus))
        }.map { ps: Iterator[(Mat, Int)] =>
          ps.reduce(plus)
        },
        Duration.Inf)
*/

      sum.consume(_ / new Scalar(cnt, cnt, cnt))
    } else {
      zeros(0, 0, CvType.CV_32SC3)
    }
  }

}
