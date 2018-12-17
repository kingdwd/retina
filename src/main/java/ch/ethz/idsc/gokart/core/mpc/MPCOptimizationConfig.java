// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.sys.AppResources;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

public class MPCOptimizationConfig {
  public static final MPCOptimizationConfig GLOBAL = AppResources.load(new MPCOptimizationConfig());
  /***************************************************/
  /** The limit for the norm of the acceleration */
  public Scalar maxAcceleration = Quantity.of(1, SI.ACCELERATION);
  /** The limit for the Go-kart speed */
  public Scalar maxSpeed = Quantity.of(10, SI.VELOCITY);
  /** The mpc update cycle time when not successful */
  public Scalar updateCycle = Quantity.of(0.2, SI.SECOND);
  /** The wait time after a successful optimization */
  public Scalar updateDelay = Quantity.of(0.0, SI.SECOND);
  /** Steering anti-lag */
  public Scalar steerAntiLag = Quantity.of(0.4, SI.SECOND);
  /** Braking anti-lag */
  public Scalar brakingAntiLag = Quantity.of(0.1, SI.SECOND);
}
