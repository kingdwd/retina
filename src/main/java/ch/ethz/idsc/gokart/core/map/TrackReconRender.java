// code by mh, jph
package ch.ethz.idsc.gokart.core.map;

import java.awt.Graphics2D;
import java.util.Optional;

import ch.ethz.idsc.gokart.core.mpc.MPCBSplineTrack;
import ch.ethz.idsc.gokart.core.mpc.MPCBSplineTrackListener;
import ch.ethz.idsc.gokart.gui.top.TrackRender;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;

public class TrackReconRender implements RenderInterface, MPCBSplineTrackListener {
  private final TrackRender trackRender = new TrackRender();

  @Override
  public void mpcBSplineTrack(Optional<MPCBSplineTrack> optional) {
    trackRender.setTrack(optional.map(MPCBSplineTrack::bSplineTrack).orElse(null));
  }

  @Override // from RenderInterface
  public void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    trackRender.render(geometricLayer, graphics);
    // if (Objects.nonNull(lastTrack))
    // trackRender.render(geometricLayer, graphics);
    // else
    // TODO reenable rendering of initial guess
    // initialGuess.render(geometricLayer, graphics);
  }
}
