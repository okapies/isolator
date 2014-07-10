import scala.testing.Benchmark

import org.opencv.core.Core

object IsolatorOpenCVApp extends App {

  import opencv._

  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  args match {
    case Array(inFile, outFile) =>
      System.err.println("Elapsed: " + new Benchmark {
        def run = saveImage(outFile, loadVideo(inFile)(mean))
      }.runBenchmark(1))
    case _ =>
      System.err.println("Usage: isolator <input> <output>")
  }

}
