// code by jph
package ch.ethz.idsc.demo.gz;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.demo.GokartLogFile;
import ch.ethz.idsc.tensor.io.HomeDirectory;
import lcm.logging.LogPlayer;
import lcm.logging.LogPlayerConfig;

/* package */ enum GokartLcmLogPlayer {
  ;
  public static void main(String[] args) throws IOException {
    LogPlayerConfig cfg = new LogPlayerConfig();
    File file;
    file = HomeDirectory.file("temp/20180108T165210_manual.lcm");
    file = HomeDirectory.file("gokart/twist/20180108T165210_4/log.lcm");
    file = HomeDirectory.file("gokart/short/20180108T165210_1/log.lcm");
    file = HomeDirectory.file("gokart/pursuit/20180112T154355/log.lcm");
    file = HomeDirectory.file("gokart/manual/20180108T154035/log.lcm");
    file = HomeDirectory.file("Downloads/logs/log.lcm");
    file = GioeleLogFileLocator.file(GokartLogFile._20180509T120343_8d5acc24);
    cfg.logFile = file.toString();
    cfg.speed_numerator = 1;
    cfg.speed_denominator = 2;
    LogPlayer.create(cfg);
  }
}
