import org.opencv.highgui.{Highgui, VideoCapture}

import org.opencv.core.Core

object IsolatorOpenCVApp extends App {

  import opencv._

  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  args match {
    case Array(inFile, outFile) =>
      val start = System.currentTimeMillis()

      using (new VideoCapture(inFile)) { cap =>
        val frames = cap.takeWhile(_ != None).map(_.get)
        Highgui.imwrite(outFile, mean(frames))
      }

      System.err.println(s"Elapsed: ${System.currentTimeMillis() - start}")
    case _ =>
      System.err.println("Usage: isolator <input> <output>")
  }

}
