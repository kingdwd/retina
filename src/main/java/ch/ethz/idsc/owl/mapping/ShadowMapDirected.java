// code by ynager
package ch.ethz.idsc.owl.mapping;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.helper.opencv_core.AbstractScalar;

import ch.ethz.idsc.owl.bot.se2.LidarEmulator;
import ch.ethz.idsc.owl.data.img.CvHelper;
import ch.ethz.idsc.owl.gui.win.AffineTransforms;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.owl.math.region.ImageRegion;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.sophus.hs.r2.Se2Bijection;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.ResourceData;

// TODO_YN extract more duplicate code from ShadowMapSpherical into common base class  
public class ShadowMapDirected extends ShadowMapCV {
  private final static int NSEGS = 40;
  private final static float CAR_RAD = 1.0f;
  // ---
  private final Mat initArea;
  private final Mat shadowArea;
  private final Mat obsDilArea;
  private final Mat ellipseKernel = //
      opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(5, 5));
  private final float vMax;
  private final List<Mat> laneMasks = new ArrayList<>();
  private final List<Mat> updateKernels = new ArrayList<>();
  private final List<Mat> carKernels = new ArrayList<>();

  public ShadowMapDirected(LidarEmulator lidarEmulator, ImageRegion imageRegion, String lanes, float vMax) {
    super(lidarEmulator, imageRegion);
    this.vMax = vMax;
    // setup
    BufferedImage carLanesImg = ResourceData.bufferedImage(lanes);
    Mat img = CvHelper.bufferedImageToMat(carLanesImg);
    URL kernelURL = getClass().getResource("/cv/kernels/kernel4.bmp");
    float pixelPerSeg = 253.0f / (NSEGS);
    int[] limits = IntStream.rangeClosed(0, NSEGS).map(i -> (int) (i * pixelPerSeg)).toArray();
    Mat kernOrig = opencv_imgcodecs.imread(kernelURL.getPath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
    Mat carKernel = Mat.ones(new Size(5, 5), kernOrig.type()).asMat();
    byte[] a = { //
        0, 1, 1, 1, 0, //
        0, 1, 1, 1, 0, //
        0, 1, 1, 1, 0, //
        0, 1, 1, 1, 0, //
        0, 1, 1, 1, 0 };
    carKernel.data().put(a);
    //
    // build rotated kernels
    for (int s = 0; s < NSEGS; s++) {
      Mat img0 = Mat.zeros(img.size(), img.type()).asMat();
      opencv_imgproc.threshold(img, img0, limits[s], 255, opencv_imgproc.THRESH_TOZERO);
      opencv_imgproc.threshold(img0, img0, limits[s + 1], 255, opencv_imgproc.THRESH_TOZERO_INV);
      opencv_imgproc.threshold(img0, img0, 0, 255, opencv_imgproc.THRESH_BINARY);
      laneMasks.add(img0.clone());
      // opencv_imgcodecs.imwrite("bcmaskabc" + s + ".jpg", img0);
      // ---
      double angle = 360.0 / (NSEGS) * s;
      Mat kern = new Mat(kernOrig.size(), kernOrig.type());
      Mat carKern = new Mat(carKernel.size(), carKernel.type());
      Mat rotMatrix1 = opencv_imgproc.getRotationMatrix2D(new Point2f(7, 7), angle + 4, 1.0); // TOOO magic consts
      Mat rotMatrix2 = opencv_imgproc.getRotationMatrix2D(new Point2f(2, 2), angle + 4, 1.0);
      opencv_imgproc.warpAffine(kernOrig, kern, rotMatrix1, kernOrig.size());
      opencv_imgproc.warpAffine(carKernel, carKern, rotMatrix2, carKernel.size());
      updateKernels.add(kern);
      carKernels.add(carKern);
    }
    // build obstacle images
    Mat obsLane = new Mat(img.size(), img.type());
    opencv_imgproc.threshold(img, obsLane, 0, 255, opencv_imgproc.THRESH_BINARY_INV);
    opencv_imgproc.dilate(obsLane, obsLane, ellipseKernel);
    obsDilArea = Mat.zeros(img.size(), img.type()).asMat();
    opencv_imgproc.dilate(obsLane, obsDilArea, ellipseKernel, new Point(-1, -1), radius2it(ellipseKernel, CAR_RAD), opencv_core.BORDER_CONSTANT, null);
    // TODO_YN use spherical for lanes, carkernel for other obstacles
    // Mat carObs = opencv_imgcodecs.imread(carObsURL.getPath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
    // IntStream.range(0, NSEGS).parallel() //
    // .mapToObj(seg -> StaticHelper.dilateSegment(seg, carObs, carKernels, new Point(2, 2), laneMasks, 15)) // TODO_YN magic iteration number
    // .forEach(upimg -> opencv_core.add(obsDilArea, upimg, obsDilArea));
    //
    initArea = new Mat(img.size(), img.type(), AbstractScalar.WHITE);
    opencv_imgproc.erode(initArea, initArea, ellipseKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);
    opencv_core.subtract(initArea, obsDilArea, initArea);
    this.shadowArea = initArea.clone();
    setColor(new Color(255, 50, 74));
  }

  @Override // from ShadowMap
  public void updateMap(StateTime stateTime, float timeDelta) {
    updateMap(shadowArea, stateTime, timeDelta);
  }

  @Override // from ShadowMap
  public final Mat getInitMap() {
    return initArea.clone();
  }

  @Override
  public void updateMap(Mat area_, StateTime stateTime, float timeDelta) {
    // get lidar polygon and transform to pixel values
    GeometricLayer world2pixelLayer = GeometricLayer.of(world2pixel);
    Mat area = area_.clone();
    Se2Bijection gokart2world = new Se2Bijection(stateTime.state());
    world2pixelLayer.pushMatrix(gokart2world.forward_se2());
    Tensor poly = lidarEmulator.getPolygon(stateTime);
    //  ---
    // transform lidar polygon to pixel values
    Point polygonPoint = StaticHelper.toPoint(poly.stream().map(world2pixelLayer::toVector));
    world2pixelLayer.popMatrix();
    // tens); // reformat polygon to point
    // ---
    // fill lidar polygon and subtract it from shadow region
    Mat lidarMat = new Mat(initArea.size(), area.type(), AbstractScalar.BLACK);
    opencv_imgproc.fillPoly(lidarMat, polygonPoint, new int[] { poly.length() }, 1, AbstractScalar.WHITE);
    opencv_core.subtract(area, lidarMat, area);
    // expand shadow region according to lane direction
    // TODO_YN this is a bottleneck
    int it = radius2it(updateKernels.get(0), timeDelta * vMax); // TODO_YN check if correct
    for (int i = 1; i < it; ++i) {
      List<Mat> updated = IntStream.range(0, NSEGS).parallel() //
          .mapToObj(s -> StaticHelper.dilateSegment(s, area, updateKernels, new Point(-1, -1), laneMasks, 1)) //
          .collect(Collectors.toList());
      updated.stream().parallel().forEach(upimg -> opencv_core.add(area, upimg, area));
      opencv_core.subtract(area, obsDilArea, area);
      opencv_core.bitwise_and(initArea, area, area);
    }
    // ---
    area.copyTo(area_);
  }

  public final Mat getCurrentMap() {
    return shadowArea.clone();
  }

  private final int radius2it(Mat spericalKernel, float radius) {
    float pixelRadius = spericalKernel.arrayWidth() / 2.0f;
    float worldRadius = pixelRadius * pixelDim.number().floatValue();
    return (int) Math.ceil(radius / worldRadius);
  }

  @Override
  public void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    final Tensor matrix = geometricLayer.getMatrix();
    AffineTransform transform = AffineTransforms.toAffineTransform(matrix.dot(pixel2world));
    Mat plotArea = getCurrentMap();
    // setup colorspace
    opencv_imgproc.cvtColor(plotArea, plotArea, opencv_imgproc.CV_GRAY2RGBA);
    Mat color = new Mat(4, 1, opencv_core.CV_8UC4);
    byte[] a = StaticHelper.toAGRB(colorShadowFill);
    color.data().put(a);
    plotArea.setTo(color, plotArea);
    //  convert to bufferedimage
    BufferedImage bufferedImage = new BufferedImage(plotArea.arrayWidth(), plotArea.arrayHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
    plotArea.data().get(data);
    graphics.drawImage(bufferedImage, transform, null);
  }

  @Override
  public float getMinTimeDelta() {
    return updateKernels.get(0).arrayWidth() * pixelDim.number().floatValue() / (2.0f * vMax);
  }

  @Override
  public Mat getShape(Mat mat, float radius) {
    // TODO_YN use car shape
    Mat shape = mat.clone();
    Mat radPx = new Mat(Scalar.all((CAR_RAD + radius + 0.5) / pixelDim.number().floatValue()));
    Mat negSrc = new Mat(shape.size(), shape.type());
    opencv_core.bitwise_not(shape, negSrc);
    opencv_imgproc.distanceTransform(negSrc, shape, opencv_imgproc.CV_DIST_L2, opencv_imgproc.CV_DIST_MASK_PRECISE);
    opencv_core.compare(shape, radPx, shape, opencv_core.CMP_LE);
    return shape;
  }
}
