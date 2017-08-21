// code by jph
package ch.ethz.idsc.retina.dev.davis;

import java.nio.ByteBuffer;

/** reads raw bytes from a source buffer, decodes them to an event.
 * the event is distributed to listeners */
public interface DavisDecoder {
  /** in raw data format, a davis event consist of 8 bytes
   * 
   * @param byteBuffer from which 8 bytes of raw data can be read */
  void read(ByteBuffer byteBuffer);

  /** in raw data format, a davis event is encoded as two 32-bit integers
   * 
   * @param data
   * @param time */
  void read(int data, int time);

  /** @param davisEventListener to subscribe to dvs, aps, or imu events */
  void addListener(DavisEventListener davisEventListener);
}
