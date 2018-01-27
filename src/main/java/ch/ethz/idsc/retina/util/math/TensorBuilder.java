// code by jph
package ch.ethz.idsc.retina.util.math;

import java.util.Deque;
import java.util.LinkedList;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** concept similar to {@link StringBuilder}
 * 
 * potentially faster than
 * {@link Tensors#empty()} with subsequent {@link Tensor#append(Tensor)}
 * 
 * underlying implementation may change as soon as a faster method is found. */
public class TensorBuilder {
  private final Deque<Tensor> deque = new LinkedList<>();

  public void append(Tensor tensor) {
    deque.add(tensor);
  }

  public Tensor getTensor() {
    return Tensor.of(deque.stream());
  }
}
