// code by mg
package ch.ethz.idsc.retina.app.slam.prc;

import ch.ethz.idsc.retina.app.slam.SlamPrcContainer;
import ch.ethz.idsc.retina.app.slam.config.SlamDvsConfig;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.sophus.crv.subdiv.BSpline2CurveSubdivision;
import ch.ethz.idsc.sophus.crv.subdiv.CurveSubdivision;
import ch.ethz.idsc.sophus.lie.rn.RnGeodesic;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Nest;

/** fits a curve through a set of points */
/* package */ class SlamCurveFitting extends AbstractSlamCurveStep {
  private static final CurveSubdivision CURVE_SUBDIVISION = new BSpline2CurveSubdivision(RnGeodesic.INSTANCE);
  // ---
  private final int iterations;

  SlamCurveFitting(SlamPrcContainer slamPrcContainer) {
    super(slamPrcContainer);
    iterations = Magnitude.ONE.toInt(SlamDvsConfig.eventCamera.slamPrcConfig.iterations);
  }

  @Override // from CurveListener
  public void process() {
    Tensor validGokartWaypoints = slamPrcContainer.getValidGokartWaypoints();
    slamPrcContainer.setFittedCurve(fitCurve(validGokartWaypoints, iterations));
  }

  /** fits a curve by applying BSpline2CurveSubdivision a variable number of times
   * 
   * @param validGokartWaypoints
   * @param iterations
   * @return fitted curve */
  private static Tensor fitCurve(Tensor validGokartWaypoints, int iterations) {
    return Nest.of(CURVE_SUBDIVISION::string, validGokartWaypoints, iterations);
  }
}
