package opencv

import org.scalacheck.{Prop, Properties}
import org.scalacheck.Gen._

import org.opencv.core.{Core, CvType, Mat, Scalar, Size}

class MatSpec extends Properties("MatSpec") {

  import Prop.forAll
  import MatGen._

  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  property("identity element") =
    forAll(matInt(choose(1, 10), choose(1, 10))) { m =>
      m.consume { m =>
        val zero = zeros(m.rows, m.cols, m.`type`)
        (m + zero, zero + m).consume { case (m1, m2) =>
          (m1 === m) && (m2 === m)
        }
      }
    }

  property("associativity") =
    forAll(matInt3(choose(1, 10), choose(1, 10))) { case (a, b, c) =>
      (
        (a + b).consume(_ + c), // (a + b) + c
        (b + c).consume(a + _)  // a + (b + c)
      ).consume(_ === _)
    }

  property("mean === mearPar") =
    forAll(
      zip(
        matIntList(
          rows = choose(1, 10),
          cols = choose(1, 10),
          length = choose(0, 10),
          ch = const(3) // TODO
        ),
        choose(1, 4) // batchSize
      )) { case (ms, batchSize) =>
      (
        mean(ms.map(_.copy()).iterator),
        meanPar(ms.map(_.copy()).iterator, batchSize)
      ).consume(_ === _)
    }

}
