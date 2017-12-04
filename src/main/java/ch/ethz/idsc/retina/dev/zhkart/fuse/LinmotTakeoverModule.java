// code by rvmoss and jph
package ch.ethz.idsc.retina.dev.zhkart.fuse;

import java.util.Optional;

import ch.ethz.idsc.retina.dev.linmot.LinmotGetEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotGetListener;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutHelper;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutProvider;
import ch.ethz.idsc.retina.dev.linmot.LinmotSocket;
import ch.ethz.idsc.retina.dev.zhkart.ProviderRank;
import ch.ethz.idsc.retina.sys.AbstractModule;
import ch.ethz.idsc.retina.sys.SafetyCritical;
import ch.ethz.idsc.retina.util.data.Watchdog;

/** module detects when human presses the break while the software
 * is controlling the break
 * 
 * module has to be stopped and restarted once fuse is blown */
@SafetyCritical
public final class LinmotTakeoverModule extends AbstractModule implements LinmotGetListener, LinmotPutProvider {
  /** in order for fuse to blow, the position discrepancy
   * has to be maintained for 0.05[s] */
  private static final long DURATION_MS = 50;
  /** position discrepancy threshold
   * anything below threshold is expected during normal operation */
  private static final double THRESHOLD_POS_DELTA = 20000;
  // ---
  private final Watchdog watchdog = new Watchdog(DURATION_MS * 1e-3);
  private boolean isBlown = false;

  @Override // from AbstractModule
  protected void first() throws Exception {
    LinmotSocket.INSTANCE.addAll(this);
  }

  @Override // from AbstractModule
  protected void last() {
    LinmotSocket.INSTANCE.removeAll(this);
  }

  /***************************************************/
  @Override // from LinmotGetListener
  public void getEvent(LinmotGetEvent getEvent) {
    if (getEvent.getPositionDiscrepancyRaw() <= THRESHOLD_POS_DELTA) // abs(int) not used
      watchdog.pacify(); // <- at nominal rate the watchdog is notified every 4[ms]
  }

  /***************************************************/
  @Override // from LinmotPutProvider
  public ProviderRank getProviderRank() {
    return ProviderRank.EMERGENCY;
  }

  @Override // from LinmotPutProvider
  public Optional<LinmotPutEvent> putEvent() {
    isBlown |= watchdog.isBlown();
    return Optional.ofNullable(isBlown ? LinmotPutHelper.OFF_MODE_EVENT : null); // deactivate break
  }
}
