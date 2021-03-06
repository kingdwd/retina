// code by jph
package ch.ethz.idsc.gokart.offline.gui;

import ch.ethz.idsc.gokart.dev.steer.SteerGetEvent;
import ch.ethz.idsc.gokart.dev.steer.SteerGetListener;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.img.ColorDataGradient;
import ch.ethz.idsc.tensor.img.ColorDataGradients;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clip;
import ch.ethz.idsc.tensor.sca.Clips;

/* package */ class SteerTsuTrqRow extends ClipLogImageRow implements SteerGetListener {
  private static final Clip CLIP = Clips.absolute(Quantity.of(1, "SCT"));
  // ---
  private Scalar scalar = RealScalar.ZERO;

  @Override // from SteerGetListener
  public void getEvent(SteerGetEvent steerGetEvent) {
    scalar = CLIP.rescale(steerGetEvent.tsuTrq());
  }

  @Override // from GokartLogImageRow
  public Scalar getScalar() {
    return scalar;
  }

  @Override // from GokartLogImageRow
  public ColorDataGradient getColorDataGradient() {
    return ColorDataGradients.THERMOMETER;
  }

  @Override // from GokartLogImageRow
  public String getName() {
    return "steer tsu torque";
  }

  @Override
  public Clip clip() {
    return CLIP;
  }
}
