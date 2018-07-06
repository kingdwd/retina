// code by vc
package ch.ethz.idsc.demo.vc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import ch.ethz.idsc.gokart.core.perc.ClusterConfig;
import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.gui.top.SensorsConfig;
import ch.ethz.idsc.owl.bot.util.UserHome;
import ch.ethz.idsc.retina.dev.lidar.VelodyneModel;
import ch.ethz.idsc.retina.lcm.OfflineLogListener;
import ch.ethz.idsc.retina.lcm.OfflineLogPlayer;
import ch.ethz.idsc.retina.lcm.lidar.VelodyneLcmChannels;
import ch.ethz.idsc.retina.lcm.lidar.Vlp16LcmHandler;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

enum ClusterEvaluation {
  ;
  private static final String CHANNEL_VLP16 = VelodyneLcmChannels.ray(VelodyneModel.VLP16, "center");

  public static void main(String[] args) throws IOException {
    Vlp16LcmHandler vlp16LcmHandler = SensorsConfig.GLOBAL.vlp16LcmHandler();
    ClusterConfig clusterConfig = new ClusterConfig();
    clusterConfig.epsilon = Quantity.of(0.3, SI.METER);
    ClusterAreaEvaluationListener clusterEvaluationListener = new ClusterAreaEvaluationListener(clusterConfig);
    vlp16LcmHandler.lidarAngularFiringCollector.addListener(clusterEvaluationListener.lidarClustering);
    OfflineLogListener offlineLogListener = new OfflineLogListener() {
      @Override
      public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
        if (channel.equals(CHANNEL_VLP16))
          vlp16LcmHandler.velodyneDecoder.lasers(byteBuffer);
        else //
        if (channel.equals(GokartLcmChannel.POSE_LIDAR)) {
          GokartPoseEvent gpe = new GokartPoseEvent(byteBuffer);
          clusterEvaluationListener.setPose(gpe.getPose());
        }
      }
    };
    File file = UserHome.file("Desktop/ETHZ/log/pedestrian/20180412T163855/log.lcm");
    if (UserHome.file("").getName().equals("datahaki"))
      file = UserHome.file("gokart/pedestrian/20180412T163855/log.lcm");
    OfflineLogPlayer.process(file, offlineLogListener);
  }
}
