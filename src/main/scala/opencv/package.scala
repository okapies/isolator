import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.VideoCapture

package object opencv {

  def using[A](m1: Mat)(f: Mat => A): A = {
    try {
      f(m1)
    } finally {
      m1.release()
    }
  }

  def using[A](m1: Mat, m2: Mat)(f: (Mat, Mat) => A): A = {
    try {
      f(m1, m2)
    } finally {
      m1.release()
      m2.release()
    }
  }

  def using[A](m1: Mat, m2: Mat, m3: Mat)(f: (Mat, Mat, Mat) => A): A = {
    try {
      f(m1, m2, m3)
    } finally {
      m1.release()
      m2.release()
      m3.release()
    }
  }

  def using[A](m1: Mat, m2: Mat, m3: Mat, m4: Mat)(f: (Mat, Mat, Mat, Mat) => A): A = {
    try {
      f(m1, m2, m3, m4)
    } finally {
      m1.release()
      m2.release()
      m3.release()
      m4.release()
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

    def +=(other: Mat): Mat = {
      Core.add(self, other, self)

      self
    }

    def -(other: Mat): Mat = {
      val dst = new Mat
      Core.subtract(self, other, dst)

      dst
    }

    def -=(other: Mat): Mat = {
      Core.subtract(self, other, self)

      self
    }

    def *(other: Mat): Mat = {
      val dst = new Mat
      Core.multiply(self, other, dst)

      dst
    }

    def *=(other: Mat): Mat = {
      Core.multiply(self, other, self)

      self
    }

    def /(other: Scalar): Mat = {
      val dst = new Mat
      Core.divide(self, other, dst)

      dst
    }

    def /=(other: Scalar): Mat = {
      Core.divide(self, other, self)

      self
    }

    def ===(other: Mat): Boolean = other match {
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

    def =/=(other: Mat): Boolean = !(self === other)

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

  implicit class RichMatTuple4(val self: (Mat, Mat, Mat, Mat)) extends AnyVal {
    def consume[A](f: (Mat, Mat, Mat, Mat) => A): A = using(self._1, self._2, self._3, self._4)(f)
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

  def mean(ms: Iterator[Mat]): Mat =
    if (ms.hasNext) {
      val imdType = CvType.CV_32SC3 // TODO

      def widen(m: Mat) = m.consume(_.convertTo(imdType))
      def plus(a: (Mat, Int), b: (Mat, Int)) = (a._1, b._1).consume(_ + _) -> (a._2 + b._2)

      val (sum, cnt) = ms.map(widen(_) -> 1).reduce(plus)

      sum.consume(_ / new Scalar(cnt, cnt, cnt))
    } else {
      new Mat
    }

  def meanPar(ms: Iterator[Mat], batchSize: Int): Mat =
    if (ms.hasNext) {
      val imdType = CvType.CV_32SC3 // TODO

      def widen(m: Mat) = m.consume(_.convertTo(imdType))
      def plus(a: (Mat, Int), b: (Mat, Int)) = (a._1, b._1).consume(_ + _) -> (a._2 + b._2)

      val partitions = ms.grouped(batchSize)
      val (sum, cnt) =
        partitions
          .map(_.par.map(widen(_) -> 1).reduce(plus))
          .reduce(plus)

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
      new Mat
    }

}
