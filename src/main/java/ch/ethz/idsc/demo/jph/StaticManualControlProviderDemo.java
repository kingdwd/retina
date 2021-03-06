// code by jph
package ch.ethz.idsc.demo.jph;

import java.util.Optional;

import ch.ethz.idsc.gokart.core.man.ManualConfig;
import ch.ethz.idsc.retina.joystick.ManualControlInterface;
import ch.ethz.idsc.retina.joystick.ManualControlProvider;

/** display joystick/manual control status in console */
/* package */ enum StaticManualControlProviderDemo {
  ;
  public static void main(String[] args) throws Exception {
    ManualControlProvider manualControlProvider = ManualConfig.GLOBAL.getProvider();
    for (int index = 0; index < 50; ++index) {
      Optional<ManualControlInterface> optional = manualControlProvider.getManualControl();
      System.out.println(optional.isPresent() ? optional.get() : "no control");
      Thread.sleep(250);
    }
    System.out.println("end");
  }
}
