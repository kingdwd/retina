// code by ynager, gjoel
package ch.ethz.idsc.gokart.core.map;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ch.ethz.idsc.gokart.calib.SensorsConfig;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.win.AffineTransforms;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.owl.math.RadiusXY;
import ch.ethz.idsc.retina.util.pose.PoseHelper;
import ch.ethz.idsc.sophus.lie.se2.Se2Matrix;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.VectorQ;
import ch.ethz.idsc.tensor.mat.DiagonalMatrix;
import ch.ethz.idsc.tensor.mat.IdentityMatrix;
import ch.ethz.idsc.tensor.sca.Floor;

/** base class for occupancy grid that is renderable into map
 *
 * the cascade of affine transformation is
 * lidar2cell == grid2gcell * world2grid * gokart2world * lidar2gokart */
public class ImageGrid implements OccupancyGrid, RenderInterface {
  protected static final byte MASK_OCCUPIED = 0;
  protected static final Color COLOR_OCCUPIED = Color.BLACK;
  protected static final Color COLOR_UNKNOWN = Color.WHITE;
  // ---
  protected final Tensor lidar2gokart = SensorsConfig.GLOBAL.vlp16Gokart(); // from lidar frame to gokart frame
  protected final Scalar cellDim; // [m] per cell
  protected final Scalar cellDimInv;
  protected final Tensor gridSize;
  protected final BufferedImage obstacleImage;
  protected final byte[] imagePixels;
  protected final Graphics2D imageGraphics;
  /** 3 x 3 diagonal matrix */
  protected final Tensor scaling;
  // ---
  protected GeometricLayer lidar2cellLayer;
  protected GeometricLayer world2cellLayer;
  protected Tensor lbounds;
  protected Tensor gokart2world = null;

  /** @param lbounds vector of length 2
   * @param rangeCeil effective size of grid in coordinate space of the form {value, value}
   * @param dimension of grid in cell space */
  // TODO JPH might be better as tensor
  /* package */ ImageGrid(Tensor lbounds, Tensor rangeCeil, Dimension dimension) {
    VectorQ.requireLength(rangeCeil, 2);
    System.out.println("Grid range: " + rangeCeil);
    System.out.println("Grid size: " + dimension);
    this.lbounds = VectorQ.requireLength(lbounds, 2);
    gridSize = Tensors.vector(dimension.width, dimension.height).unmodifiable();
    cellDim = RadiusXY.requireSame(rangeCeil).divide(gridSize.Get(0));
    cellDimInv = cellDim.reciprocal();
    scaling = DiagonalMatrix.of(cellDim, cellDim, RealScalar.ONE).unmodifiable();
    // ---
    obstacleImage = new BufferedImage(dimX(), dimY(), BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster writableRaster = obstacleImage.getRaster();
    DataBufferByte dataBufferByte = (DataBufferByte) writableRaster.getDataBuffer();
    imagePixels = dataBufferByte.getData();
    imageGraphics = obstacleImage.createGraphics();
    imageGraphics.setColor(COLOR_OCCUPIED);
    imageGraphics.fillRect(0, 0, obstacleImage.getWidth(), obstacleImage.getHeight());
    // ---
    Tensor grid2cell = DiagonalMatrix.of(cellDimInv, cellDimInv, RealScalar.ONE);
    Tensor world2grid = getWorld2grid();
    //  ---
    lidar2cellLayer = GeometricLayer.of(grid2cell); // grid 2 cell
    lidar2cellLayer.pushMatrix(world2grid); // world to grid
    lidar2cellLayer.pushMatrix(IdentityMatrix.of(3)); // placeholder gokart2world
    lidar2cellLayer.pushMatrix(lidar2gokart); // lidar to gokart
    // ---
    world2cellLayer = GeometricLayer.of(grid2cell); // grid 2 cell
    world2cellLayer.pushMatrix(world2grid); // world to grid
  }

  /** @return grid size in x direction */
  protected final int dimX() {
    return gridSize.Get(0).number().intValue();
  }

  /** @return grid size in y direction */
  protected final int dimY() {
    return gridSize.Get(1).number().intValue();
  }

  /** @return Stream of all index combinations as Tensors */
  // TODO JPH function is non-sense
  protected final Stream<Tensor> cells() {
    return IntStream.range(0, dimX()).mapToObj(x -> IntStream.range(0, dimY()).mapToObj(y -> //
    Tensors.vector(x, y))).flatMap(Function.identity());
  }

  protected final Tensor getWorld2grid() {
    return Se2Matrix.of(lbounds.negate().append(RealScalar.ZERO));
  }

  /** set vehicle pose w.r.t world frame
   * @param pose vector of the form {px, py, heading} */
  public synchronized final void setPose(Tensor pose) {
    gokart2world = PoseHelper.toSE2Matrix(pose);
    lidar2cellLayer.popMatrix();
    lidar2cellLayer.popMatrix();
    lidar2cellLayer.pushMatrix(gokart2world);
    lidar2cellLayer.pushMatrix(lidar2gokart);
  }

  /** function is used as key
   * @param pos vector of the form {px, py, ...}; only the first two entries are considered
   * @return Tensor {pix, piy} */
  protected final Tensor lidarToCell(Tensor pos) {
    return Floor.of(lidar2cellLayer.toVector(pos));
  }

  /** Remark: values in the open interval (-1, 0) are now incorrectly ceil'ed to 0.
   * however, the consequences are negligible
   *
   * @param pos
   * @return Tensor {pix, piy} */
  private final Tensor worldToCell(Tensor pos) {
    Point2D point2D = world2cellLayer.toPoint2D(pos);
    return Tensors.vector((int) point2D.getX(), (int) point2D.getY());
  }

  /** @param cell Tensor {pix, piy}
   * @return byte array index */
  protected final int cellToIdx(Tensor cell) {
    return cellToIdx( //
        cell.Get(0).number().intValue(), //
        cell.Get(1).number().intValue());
  }

  /** @param pix of cell
   * @param piy of cell
   * @return byte array index */
  protected final int cellToIdx(int pix, int piy) {
    return piy * dimX() + pix;
  }

  @Override // from OccupancyGrid
  public final Tensor getGridSize() {
    return gridSize;
  }

  /** @param cell Tensor {pix, piy}
   * @return whether cell is in grid */
  protected final boolean isCellInGrid(Tensor cell) {
    return isCellInGrid( //
        cell.Get(0).number().intValue(), //
        cell.Get(1).number().intValue());
  }

  /** @param pix of cell
   * @param piy of cell
   * @return whether cell is in grid */
  protected final boolean isCellInGrid(int pix, int piy) {
    return 0 <= pix && pix < dimX() //
        && 0 <= piy && piy < dimY();
  }

  @Override // from OccupancyGrid
  public final Tensor getTransform() {
    Tensor translate = IdentityMatrix.of(3);
    translate.set(lbounds.get(0).multiply(cellDimInv), 0, 2);
    translate.set(lbounds.get(1).multiply(cellDimInv), 1, 2);
    return scaling.dot(translate);
  }

  public final boolean isCellOccupied(Tensor cell) {
    return isCellOccupied( //
        cell.Get(0).number().intValue(), //
        cell.Get(1).number().intValue());
  }

  @Override // from OccupancyGrid
  public final boolean isCellOccupied(int pix, int piy) {
    return isCellInGrid(pix, piy) //
        && imagePixels[cellToIdx(pix, piy)] == MASK_OCCUPIED;
  }

  @Override // from OccupancyGrid
  public void clearStart(int startX, int startY, double orientation) {
    // ---
  }

  @Override // from Region<Tensor>
  public final boolean isMember(Tensor state) {
    return isCellOccupied(worldToCell(state));
  }

  @Override // from RenderInterface
  public synchronized final void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    // similar to ImageRender?
    graphics.drawImage(obstacleImage, AffineTransforms.toAffineTransform( //
        geometricLayer.getMatrix().dot(getTransform())), null);
  }
}
