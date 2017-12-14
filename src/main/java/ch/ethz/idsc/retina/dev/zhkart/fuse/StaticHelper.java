// code by jph
package ch.ethz.idsc.retina.dev.zhkart.fuse;

import java.nio.FloatBuffer;

import ch.ethz.idsc.retina.dev.steer.SteerColumnInterface;
import ch.ethz.idsc.retina.dev.steer.SteerConfig;
import ch.ethz.idsc.retina.gui.gokart.top.ChassisGeometry;
import ch.ethz.idsc.retina.gui.gokart.top.SensorsConfig;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensors;

public enum StaticHelper {
  ;
  /* package */ static boolean isPathObstructed( //
      SteerColumnInterface steerColumnInterface, FloatBuffer floatBuffer) {
    if (steerColumnInterface.isSteerColumnCalibrated()) {
      Scalar angle = SteerConfig.GLOBAL.getAngleFromSCE(steerColumnInterface); // <- calibration checked
      return isPathObstructed(angle, floatBuffer);
    }
    return true;
  }

  /** @param angle without unit but interpretation as radians
   * @param floatBuffer
   * @return */
  /* package */ static boolean isPathObstructed(Scalar angle, FloatBuffer floatBuffer) {
    final int position = floatBuffer.position();
    final int size = floatBuffer.limit() / 2; // dimensionality of point: planar lidar
    // ---
    Scalar half = ChassisGeometry.GLOBAL.yHalfWidthMeter();
    ClearanceTracker clearanceTracker = new ClearanceTracker(half, angle, SensorsConfig.GLOBAL.urg04lx);
    // ---
    for (int index = 0; index < size; ++index) {
      float px = floatBuffer.get(); // TODO use unit m
      float py = floatBuffer.get();
      clearanceTracker.feed(Tensors.vector(px, py));
    }
    floatBuffer.position(position);
    return clearanceTracker.violation().isPresent();
  }
}
