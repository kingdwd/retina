// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.retina.util.TravisUserName;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Timing;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.QuantityTensor;
import junit.framework.TestCase;

public class MPCBSplineTrackTest extends TestCase {
  public void testQuery3() {
    Tensor ctrX = QuantityTensor.of(Tensors.vector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    Tensor ctrY = QuantityTensor.of(Tensors.vector(3, 4, 5, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    Tensor ctrR = QuantityTensor.of(Tensors.vector(6, 7, 8, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    MPCBSplineTrack mpcbSplineTrack = new MPCBSplineTrack(Transpose.of(Tensors.of(ctrX, ctrY, ctrR)), true);
    Timing timing = Timing.started();
    mpcbSplineTrack.getPathParameterPreview(5, Tensors.vector(0, 3).multiply(Quantity.of(1, SI.METER)), Quantity.of(0, SI.METER));
    long duration = timing.nanoSeconds();
    System.out.println(duration);
    long limit = TravisUserName.whoami() ? 20_500_000 : 3_400_000;
    assertTrue(duration < limit);
  }
}
