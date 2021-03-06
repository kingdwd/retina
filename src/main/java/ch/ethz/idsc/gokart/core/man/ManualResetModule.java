// code by jph
package ch.ethz.idsc.gokart.core.man;

import java.util.Optional;

import ch.ethz.idsc.gokart.dev.GokartActuatorCalibration;
import ch.ethz.idsc.retina.joystick.ManualControlInterface;
import ch.ethz.idsc.retina.joystick.ManualControlProvider;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.sys.AbstractClockedModule;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

/** the module monitors the reset button of the joystick.
 * when the button is pressed by the operator, the module schedules
 * the calibration procedure for the devices that are not calibrated.
 * the devices are: misc, linmot, and steer. */
public final class ManualResetModule extends AbstractClockedModule {
  private final ManualControlProvider manualControlProvider = ManualConfig.GLOBAL.getProvider();

  @Override // from AbstractClockedModule
  protected void runAlgo() {
    Optional<ManualControlInterface> optional = manualControlProvider.getManualControl();
    if (optional.isPresent() && //
        optional.get().isResetPressed()) // manual control reset button pressed
      GokartActuatorCalibration.all();
  }

  @Override // from AbstractClockedModule
  protected void first() {
    // ---
  }

  @Override // from AbstractClockedModule
  protected void last() {
    // ---
  }

  @Override // from AbstractClockedModule
  protected Scalar getPeriod() {
    return Quantity.of(0.1, SI.SECOND);
  }
}
