import org.opencv.core.Core

object IsolatorOpenCVApp extends App {

  import opencv._

  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  args match {
    case Array(inFile, outFile) =>
      loadVideo(inFile)(fs => saveImage(outFile, mean(fs)))
    case _ =>
      System.err.println("Usage: isolator <input> <output>")
  }

}
