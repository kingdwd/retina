// code by jph
package ch.ethz.idsc.gokart.lcm;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.ethz.idsc.gokart.core.man.ManualConfig;
import ch.ethz.idsc.gokart.dev.u3.GokartLabjackFrame;
import ch.ethz.idsc.gokart.dev.u3.GokartLabjackLcmClient;
import ch.ethz.idsc.gokart.dev.u3.StaticManualControlProvider;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.retina.joystick.JoystickDecoder;
import ch.ethz.idsc.retina.joystick.ManualControlInterface;
import ch.ethz.idsc.retina.joystick.ManualControlListener;

/** Careful:
 * do not use ManualControlLcmClient for control in live-operation!
 * use ManualControlLcmClient only for visualization, or in post-processing playback
 * 
 * <p>Hint: for control of the gokart in live-operatio, use
 * {@link ManualConfig}
 * {@link StaticManualControlProvider}
 * {@link GokartLabjackLcmClient} */
public final class ManualControlLcmClient implements LcmClientInterface {
  private final List<ManualControlListener> listeners = new CopyOnWriteArrayList<>();
  private final BinaryLcmClient joystickLcmClient = new BinaryLcmClient(GokartLcmChannel.JOYSTICK) {
    @Override // from BinaryLcmClient
    protected void messageReceived(ByteBuffer byteBuffer) {
      ManualControlInterface manualControlInterface = (ManualControlInterface) JoystickDecoder.decode(byteBuffer);
      listeners.forEach(l -> l.manualControl(manualControlInterface));
    }
  };
  private final BinaryLcmClient labjackLcmClient = new BinaryLcmClient(GokartLcmChannel.LABJACK_U3_ADC) {
    @Override // from BinaryLcmClient
    protected void messageReceived(ByteBuffer byteBuffer) {
      ManualControlInterface manualControlInterface = new GokartLabjackFrame(byteBuffer);
      listeners.forEach(listener -> listener.manualControl(manualControlInterface));
    }
  };

  @Override // from LcmClientInterface
  public void startSubscriptions() {
    joystickLcmClient.startSubscriptions();
    labjackLcmClient.startSubscriptions();
  }

  @Override // from LcmClientInterface
  public void stopSubscriptions() {
    joystickLcmClient.stopSubscriptions();
    labjackLcmClient.stopSubscriptions();
  }

  public void addListener(ManualControlListener manualControlListener) {
    listeners.add(manualControlListener);
  }
}
