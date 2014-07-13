import scala.testing.Benchmark

import org.opencv.core.Core

object IsolatorOpenCVApp extends App {

  import opencv._

  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  args match {
    case Array(inFile, outFile) =>
      val start = System.currentTimeMillis()

      saveImage(outFile, loadVideo(inFile)(mean))

      System.err.println(s"Elapsed: ${System.currentTimeMillis() - start}")
    case _ =>
      System.err.println("Usage: isolator <input> <output>")
  }

}
