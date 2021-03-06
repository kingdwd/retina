// code by ynager adapted by mh
package ch.ethz.idsc.demo.mh;

import java.awt.image.BufferedImage;
import java.io.File;

import ch.ethz.idsc.gokart.core.map.MappingConfig;
import ch.ethz.idsc.gokart.core.slam.LocalizationConfig;
import ch.ethz.idsc.gokart.core.slam.LocalizationMaps;
import ch.ethz.idsc.gokart.lcm.OfflineLogPlayer;
import ch.ethz.idsc.gokart.offline.map.MappingAnalysisOffline;
import ch.ethz.idsc.retina.util.io.PngAnimationWriter;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.io.AnimationWriter;
import ch.ethz.idsc.tensor.io.HomeDirectory;
import ch.ethz.idsc.tensor.qty.Quantity;

/** generates and exports sequence of B/W images of occupancy grid */
/* package */ enum RunMappingAnalysisOffline {
  ;
  public static void main(String[] args) throws Exception {
    LocalizationConfig localizationConfig = new LocalizationConfig();
    localizationConfig.predefinedMap = LocalizationMaps.DUBILAB_20190314.name();
    File file = new File("/media/datahaki/data/gokart/0701map/20190701/20190701T174152_00", "log.lcm");
    try (AnimationWriter animationWriter = new PngAnimationWriter(HomeDirectory.Pictures("mapperHR"))) {
      MappingConfig mappingConfig = new MappingConfig();
      mappingConfig.obsRadius = Quantity.of(0.8, SI.METER);
      // MappingConfig.GLOBAL.P_M = RealScalar.of(0.95);
      OfflineLogPlayer.process(file, new MappingAnalysisOffline(localizationConfig, mappingConfig, Quantity.of(1, SI.SECOND)) {
        @Override
        public void accept(BufferedImage bufferedImage) {
          try {
            animationWriter.write(bufferedImage);
          } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException();
          }
        }
      });
    }
    System.out.print("Done.");
  }
}
