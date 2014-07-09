import org.opencv.core.{Core, CvType, Mat, Scalar, Size}
import org.opencv.highgui.{Highgui, VideoCapture}

package object opencv {

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

  def mean(frames: Iterator[Mat]): Mat =
    if (frames.hasNext) {
      val head = convertTo(CvType.CV_64FC3)(frames.next)
      val (count, out) =
        frames.
          map(convertTo(CvType.CV_64FC3)).
          foldLeft((1, head)) { case ((cnt, sum), f) => (cnt + 1, add(sum, f)) }

      divide(out, new Scalar(count, count, count))
    } else {
      new Mat
    }

  def loadVideo[A](filename: String)(f: Iterator[Mat] => A): A = {
    val cap = new VideoCapture(filename)
    try {
      f(Iterator.continually(nextFrame(cap)).takeWhile(_ != None).map(_.get))
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
