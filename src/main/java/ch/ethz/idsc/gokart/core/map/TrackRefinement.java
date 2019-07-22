// code by mh
package ch.ethz.idsc.gokart.core.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.math.UniformBSpline2;
import ch.ethz.idsc.sophus.flt.ga.Regularization2Step;
import ch.ethz.idsc.sophus.lie.rn.RnGeodesic;
import ch.ethz.idsc.sophus.math.Extract2D;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.sca.Abs;

public class TrackRefinement {
  public static abstract class TrackConstraint {
    private Tensor controlPointsX = null;
    private Tensor controlPointsY = null;
    private Tensor radiusControlPoints = null;

    public abstract void compute(Tensor controlpointsX, Tensor controlpointsY, Tensor radiusControlPoints);

    void setAll(Tensor controlPointsX, Tensor controlPointsY, Tensor radiusControlPoints) {
      this.controlPointsX = controlPointsX;
      this.controlPointsY = controlPointsY;
      this.radiusControlPoints = radiusControlPoints;
    }

    public Tensor getControlPointsX() {
      return controlPointsX;
    }

    public Tensor getControlPointsY() {
      return controlPointsY;
    }

    public Tensor getRadiusControlPoints() {
      return radiusControlPoints;
    }
  }

  public static class TrackSplitConstraint extends TrackConstraint {
    private final BSplineTrack track;
    private Scalar trackProg = null;
    private Tensor trackPos = null;
    private Tensor trackDirection = null;

    public TrackSplitConstraint(BSplineTrack track) {
      this.track = track;
    }

    @Override // from TrackConstraint
    public void compute(Tensor controlpointsX, Tensor controlpointsY, Tensor radiusControlPoints) {
      Tensor first = Tensors.of(controlpointsX.Get(0), controlpointsY.Get(0));
      Tensor second = Tensors.of(controlpointsX.Get(1), controlpointsY.Get(1));
      Tensor startPos = Mean.of(Tensors.of(first, second));
      if (Objects.isNull(trackProg) || Objects.isNull(trackPos) || Objects.isNull(trackDirection)) {
        trackProg = track.getNearestPathProgress(startPos);
        trackPos = track.getPositionXY(trackProg);
        trackDirection = track.getDirectionXY(trackProg);
      }
      Tensor realVector = second.subtract(first);
      Scalar projection = (Scalar) Max.of(realVector.dot(trackDirection), Quantity.of(0, SI.METER)).divide(RealScalar.of(2));
      Tensor correctedFirst = startPos.subtract(trackDirection.multiply(projection));
      Tensor correctedSecond = startPos.add(trackDirection.multiply(projection));
      setAll(controlpointsX, controlpointsY, radiusControlPoints);
      controlpointsX.set(correctedFirst.Get(0), 0);
      controlpointsX.set(correctedSecond.Get(0), 1);
      controlpointsY.set(correctedFirst.Get(1), 0);
      controlpointsY.set(correctedSecond.Get(1), 1);
    }
  }

  public static class PositionalStartConstraint extends TrackConstraint {
    private Tensor wantedPosition = null;
    private Tensor wantedDirection = null;

    @Override // from TrackConstraint
    public void compute(Tensor controlpointsX, Tensor controlpointsY, Tensor radiusControlPoints) {
      Tensor first = Tensors.of(controlpointsX.Get(0), controlpointsY.Get(0));
      Tensor second = Tensors.of(controlpointsX.Get(1), controlpointsY.Get(1));
      Tensor startPos = Mean.of(Tensors.of(first, second));
      if (Objects.isNull(wantedPosition)) {
        wantedPosition = startPos;
        wantedDirection = Normalize.with(Norm._2).apply(second.subtract(first));
      }
      Tensor realVector = second.subtract(first);
      Scalar projection = (Scalar) Max.of(realVector.dot(wantedDirection), Quantity.of(0, SI.METER)).divide(RealScalar.of(2));
      Tensor correctedFirst = startPos.subtract(wantedDirection.multiply(projection));
      Tensor correctedSecond = startPos.add(wantedDirection.multiply(projection));
      setAll(controlpointsX, controlpointsY, radiusControlPoints);
      controlpointsX.set(correctedFirst.Get(0), 0);
      controlpointsX.set(correctedSecond.Get(0), 1);
      controlpointsY.set(correctedFirst.Get(1), 0);
      controlpointsY.set(correctedSecond.Get(1), 1);
    }
  }

  public static class PositionalEndConstraint extends TrackConstraint {
    private Tensor wantedPosition = null;
    private Tensor wantedDirection = null;

    @Override // from TrackConstraint
    public void compute(Tensor controlpointsX, Tensor controlpointsY, Tensor radiusControlPoints) {
      int lastIndex = controlpointsX.length() - 1;
      int secondLastIndex = lastIndex - 1;
      Tensor first = Tensors.of(controlpointsX.Get(secondLastIndex), controlpointsY.Get(secondLastIndex));
      Tensor second = Tensors.of(controlpointsX.Get(lastIndex), controlpointsY.Get(lastIndex));
      Tensor startPos = Mean.of(Tensors.of(first, second));
      if (Objects.isNull(wantedPosition)) {
        wantedPosition = startPos;
        wantedDirection = Normalize.with(Norm._2).apply(second.subtract(first));
      }
      Tensor realVector = second.subtract(first);
      Scalar projection = (Scalar) Min.of(realVector.dot(wantedDirection), Quantity.of(0, SI.METER)).divide(RealScalar.of(2));
      Tensor correctedFirst = startPos.subtract(wantedDirection.multiply(projection));
      Tensor correctedSecond = startPos.add(wantedDirection.multiply(projection));
      setAll(controlpointsX, controlpointsY, radiusControlPoints);
      controlpointsX.set(correctedFirst.Get(0), secondLastIndex);
      controlpointsX.set(correctedSecond.Get(0), lastIndex);
      controlpointsY.set(correctedFirst.Get(1), secondLastIndex);
      controlpointsY.set(correctedSecond.Get(1), lastIndex);
    }
  }

  private final OccupancyGrid occupancyGrid;

  public TrackRefinement(OccupancyGrid occupancyGrid) {
    this.occupancyGrid = occupancyGrid;
  }

  private static final Scalar gdRadiusGrowth = Quantity.of(0.07, SI.METER);
  private static final Scalar gdRegularizer = RealScalar.of(0.007);
  private static final Scalar gdLimits = RealScalar.of(1);
  private static final Scalar gdRadius = RealScalar.of(1);
  private static final TensorUnaryOperator REGULARIZATION_CYCLIC = Regularization2Step.cyclic(RnGeodesic.INSTANCE, gdRegularizer);
  private static final TensorUnaryOperator REGULARIZATION_STRING = Regularization2Step.string(RnGeodesic.INSTANCE, gdRegularizer);

  Tensor getRefinedTrack(Tensor points_xyr, Scalar resolution, int iterations, boolean closed, //
      List<TrackConstraint> constraints) {
    int m = (int) (points_xyr.length() * resolution.number().doubleValue());
    int n = points_xyr.length();
    Tensor queryPositions;
    if (closed)
      queryPositions = Tensors.vector(i -> RealScalar.of((n + 0.0) * (i / (m + 0.0))), m);
    else
      // TODO MH try Subdivide.of(0, n-2, m-1) for the below
      queryPositions = Tensors.vector(i -> RealScalar.of((n - 2.0) * (i / (m - 1.0))), m - 1);
    Tensor splineMatrix = UniformBSpline2.getBasisMatrix(n, 0, closed, queryPositions);
    Tensor splineMatrixTransp = Transpose.of(splineMatrix);
    Tensor splineMatrix1Der = UniformBSpline2.getBasisMatrix(n, 1, closed, queryPositions);
    /* for(int it=0;it<iterations;it++) {
     * Tensor positions = MPCBSpline.getPositions(controlpointsX, controlpointsY, queryPositions, closed, splineMatrix);
     * Tensor sideVectors = MPCBSpline.getSidewardsUnitVectors(controlpointsX, controlpointsY, queryPositions, closed, splineMatrix1Der);
     * Tensor sideLimits = Tensors.vector((i)->getSideLimits(positions.get(i), sideVectors.get(i)), positions.length());
     * } */
    System.out.println("Iterate " + iterations + " times!");
    for (int i = 0; i < iterations; ++i) {
      Optional<Tensor> optional = getCorrectionVectors(points_xyr, //
          queryPositions, splineMatrix, splineMatrix1Der, resolution, closed);
      if (!optional.isPresent())
        return null;
      Tensor correct = optional.get();
      points_xyr = points_xyr.add(splineMatrixTransp.dot(correct));
      points_xyr.set(gdRadiusGrowth::add, Tensor.ALL, 2);
      points_xyr = closed //
          ? REGULARIZATION_CYCLIC.apply(points_xyr)
          : REGULARIZATION_STRING.apply(points_xyr);
      // ---
      if (Objects.nonNull(constraints)) {
        // TODO JPH/MH
        Tensor controlpointsX = points_xyr.get(Tensor.ALL, 0);
        Tensor controlpointsY = points_xyr.get(Tensor.ALL, 1);
        Tensor radiusCtrPoints = points_xyr.get(Tensor.ALL, 2);
        for (TrackConstraint constraint : constraints) {
          constraint.compute(controlpointsX, controlpointsY, radiusCtrPoints);
          controlpointsX = constraint.getControlPointsX();
          controlpointsY = constraint.getControlPointsY();
          radiusCtrPoints = constraint.getRadiusControlPoints();
        }
        points_xyr = Transpose.of(Tensors.of(controlpointsX, controlpointsY, radiusCtrPoints));
      }
    }
    // MPCBSplineTrack track = new MPCBSplineTrack(controlpointsX, controlpointsY, radiusCtrPoints);
    return points_xyr;
  }

  // for debugging
  // TODO JPH/MH design is bad
  private List<Tensor> freeLines = new ArrayList<>();

  /** .
   * @param points_xyr
   * @param queryPositions
   * @param basisMatrix
   * @param basisMatrix1Der
   * @param resolution
   * @param closed
   * @return */
  private Optional<Tensor> getCorrectionVectors( //
      Tensor points_xyr, Tensor queryPositions, Tensor basisMatrix, //
      Tensor basisMatrix1Der, Scalar resolution, boolean closed) {
    // ---
    Tensor positionsXYR = basisMatrix.dot(points_xyr);
    Tensor sideVectors = BSplineUtil.getSidewardsUnitVectors( //
        Tensor.of(points_xyr.stream().map(Extract2D.FUNCTION)), //
        basisMatrix1Der);
    Scalar stepsSize = Quantity.of(0.1, SI.METER);
    freeLines = new ArrayList<>();
    Tensor sideLimits = Tensors.vector(
        i -> getSideLimits(Extract2D.FUNCTION.apply(positionsXYR.get(i)), sideVectors.get(i), stepsSize, Quantity.of(1, SI.METER)), positionsXYR.length());
    boolean hasNoSolution = sideLimits.stream().anyMatch(row -> row.get(0).equals(row.get(1)));
    if (hasNoSolution)
      return Optional.empty();
    // upwardsforce
    Tensor lowClipping = Tensors.vector(i -> Max.of(sideLimits.Get(i, 0).add(positionsXYR.Get(i, 2)), Quantity.of(0, SI.METER)), queryPositions.length());
    Tensor highClipping = Tensors.vector(i -> Max.of(positionsXYR.Get(i, 2).subtract(sideLimits.Get(i, 1)), Quantity.of(0, SI.METER)), queryPositions.length());
    Tensor sideCorr = lowClipping.subtract(highClipping).multiply(gdLimits.divide(resolution));
    // Tensor posCorr = Transpose.of(sideCorr.pmul(sideVectors));
    // System.out.println("posCorr=" + Dimensions.of(posCorr));
    // Tensor radiusCorr = highClipping.add(lowClipping).multiply(gdRadius.divide(resolution)).negate();
    // // Tensor upwardsforce = Tensors.vector(list)
    // return Optional.of(Transpose.of(Tensors.of(posCorr.get(0), posCorr.get(1), radiusCorr)));
    Tensor posCorr = sideCorr.pmul(sideVectors);
    Tensor radiusCorr = highClipping.add(lowClipping).multiply(gdRadius.divide(resolution).negate());
    return Optional.of(Tensor.of(IntStream.range(0, posCorr.length()) //
        .mapToObj(i -> posCorr.get(i).append(radiusCorr.get(i)))));
  }

  private Tensor getSideLimits(Tensor pos, Tensor sidedir, Scalar stepsSize, Scalar maxSearch) {
    // find free space
    Scalar sideStep = Quantity.of(-0.001, SI.METER);
    Tensor testPosition = null;
    Tensor lowPosition;
    Tensor highPosition;
    boolean occupied = true;
    while (occupied) {
      if (Scalars.lessThan(sideStep, Quantity.of(0, SI.METER)))
        sideStep = sideStep.negate();
      else
        sideStep = sideStep.add(stepsSize).negate();
      testPosition = pos.add(sidedir.multiply(sideStep));
      occupied = occupancyGrid.isMember(testPosition);
      if (Scalars.lessThan(maxSearch, sideStep.abs()))
        return Tensors.of(RealScalar.ZERO, RealScalar.ZERO);
    }
    // search in both directions for occupied cell
    // only for debugging
    Tensor freeline = Tensors.empty();
    // negative direction
    while (!occupied && Scalars.lessThan(Abs.of(sideStep), Quantity.of(10, SI.METER))) {
      sideStep = sideStep.subtract(stepsSize);
      testPosition = pos.add(sidedir.multiply(sideStep));
      occupied = occupancyGrid.isMember(testPosition);
    }
    freeline.append(testPosition);
    lowPosition = sideStep;
    // negative direction
    occupied = false;
    while (!occupied && Scalars.lessThan(Abs.of(sideStep), Quantity.of(10, SI.METER))) {
      sideStep = sideStep.add(stepsSize);
      testPosition = pos.add(sidedir.multiply(sideStep));
      occupied = occupancyGrid.isMember(testPosition);
    }
    highPosition = sideStep;
    freeline.append(testPosition);
    freeLines.add(freeline);
    return Tensors.of(lowPosition, highPosition);
  }
}
