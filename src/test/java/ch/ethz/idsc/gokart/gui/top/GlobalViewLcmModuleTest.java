// code by jph
package ch.ethz.idsc.gokart.gui.top;

import ch.ethz.idsc.gokart.core.pure.DubendorfCurve;
import junit.framework.TestCase;

public class GlobalViewLcmModuleTest extends TestCase {
  public void testSimple() throws Exception {
    GlobalViewLcmModule globalViewLcmModule = new GlobalViewLcmModule();
    globalViewLcmModule.first();
    Thread.sleep(1000);
    globalViewLcmModule.setPlan(DubendorfCurve.OVAL);
    Thread.sleep(1000);
    globalViewLcmModule.last();
  }
}
