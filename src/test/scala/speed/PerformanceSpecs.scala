package speed

import org.specs2.mutable.Specification
import ichi.bench.Thyme

object ThymeExtras {
  implicit class ComparisonExtra(comp: Thyme.Comparison) {
    // see https://github.com/Ichoran/thyme/blob/master/bench/Thyme.scala#L1325
    def significantlyDifferent = comp.foldChangeRuntime.pfitdiff < 0.05
    def factor = comp.foldChangeRuntime.rdiff.mean
  }
}

class PerformanceSpecs extends Specification {
  sequential

  "Speedy" should {
    "make ranges as fast as while loops" in {
      "foreach counting" in {
        beSimilarlyFast("foreach counting") {
          var counter = 0
          for (i ← 1 to 1000) counter += i * i
          counter
        } {
          var counter = 0
          var i = 1
          while (i <= 1000) {
            counter += i * i
            i += 1
          }
          counter
        } {
          var counter = 0
          for (i ← 1 to 1000: Range) counter += i * i
          counter
        }
      }
      "nested counting" in {
        beSimilarlyFast("nested counting") {
          var counter = 0
          for {
            i ← 1 to 100
            j ← 1 to 100
          } counter += i * j
          counter
        } {
          var counter = 0
          var i = 1
          while (i <= 100) {
            var j = 1
            while (j <= 100) {
              counter += i * j
              j += 1
            }

            i += 1
          }
          counter
        } {
          var counter = 0
          for {
            i ← 1 to 100: Range
            j ← 1 to 100: Range
          } counter += i * j
          counter
        }
      }
      "summing" in {
        pending("macro shouldn't apply here because Range.sum is optimized but still does")

        beSimilarlyFast("summing") {
          (1 to 1000).sum
        } {
          var counter = 0
          var i = 1
          while (i <= 1000) {
            counter += i
            i += 1
          }
          counter
        } {
          (1 to 1000: Range).sum
        }
      }
      "filtered summing" in {
        beSimilarlyFast("filtered summing") {
          (1 to 1000).filter(_ % 3 == 0).sum
        } {
          var counter = 0
          var i = 1
          while (i <= 1000) {
            if (i % 3 == 0) counter += i
            i += 1
          }
          counter
        } {
          (1 to 1000: Range).filter(_ % 3 == 0).sum
        }
      }
      "mapped summing" in {
        beSimilarlyFast("mapped summing") {
          (1 to 1000).map(i ⇒ i * i).sum
        } {
          var counter = 0
          var i = 1
          while (i <= 1000) {
            counter += i * i
            i += 1
          }
          counter
        } {
          (1 to 1000: Range).map(i ⇒ i * i).sum
        }
      }
    }
    "improve speed of Array.foreach" in {
      "array foreach counting" in {
        val array = Array.tabulate[Int](1000)(identity)
        beSimilarlyFast("array foreach counting") {
          var counter = 0
          for (x ← array) counter += x * x
          counter
        } {
          var counter = 0
          var i = 0
          while (i < array.length) {
            val x = array(i)
            counter += x * x
            i += 1
          }
          counter
        } {
          var counter = 0
          for (x ← Predef.wrapIntArray(array)) counter += x * x
          counter
        }
      }
    }
  }

  import ThymeExtras._
  val th = ichi.bench.Thyme.warmed(verbose = print)
  def beSimilarlyFast[T](name: String)(speedy: ⇒ T)(whileLoopy: ⇒ T)(rangy: ⇒ T) = {
    val result = th.benchOffPair(title = s"$name speedy vs. while", targetTime = 1)(speedy)(whileLoopy)._2
    val result2 = th.benchOffPair(title = s"$name old-style vs. while", targetTime = 1)(rangy)(whileLoopy)._2
    val speedySpeedup = 1d / result.factor * 100d
    val rangeSpeedup = 1d / result2.factor * 100d

    val nameString = s"[$name](#${name.replaceAll(" ", "-")}})"

    println(f"|$nameString%s | 100 %% | $speedySpeedup%5.2f %% | $rangeSpeedup%5.2f %%")

    !result.significantlyDifferent || result.factor > 0.99 must beTrue.or(failure("Wasn't matching as fast but " + result))
  }

}
