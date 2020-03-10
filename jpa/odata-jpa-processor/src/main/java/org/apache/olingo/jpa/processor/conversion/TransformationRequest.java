package org.apache.olingo.jpa.processor.conversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An instance of this class describes the target of an requested transformation.
 */
public final class TransformationRequest<Input, Output> extends AbstractTransformationDescription<Input, Output> {

  private final Comparable<?>[] constraints;

  /**
   *
   * @param outputType The output type of {@link Transformation}.
   * @param constraints Optional list of constraints. The order of constraints must match the expectations of
   * transformation (order of registration must be the same as order of usage request).
   */
  public TransformationRequest(final Class<Input> inputType, final Class<Output> outputType,
      final Comparable<?>... constraints) {
    super(inputType, outputType);
    if (constraints != null && constraints.length > 0) {
      this.constraints = constraints;
      if (Arrays.asList(constraints).stream().anyMatch(c -> (c == null))) {
        throw new IllegalArgumentException("all contraints must not be null");
      }
    } else {
      this.constraints = null;
    }
  }

  public List<Comparable<?>> getConstraints() {
    if (constraints == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(constraints);
  }

  public int getNumberOfConstraints() {
    if (constraints == null) {
      return 0;
    }
    return constraints.length;
  }

  @Override
  public String toString() {
    return "TransformationDescription [" + (getInputType() != null ? "inputType=" + getInputType() + ", " : "")
        + (getOutputType() != null ? "outputType=" + getOutputType() + ", " : "") + (constraints != null
        ? "constraints=" + Arrays
            .toString(constraints) : "") + "]";
  }

}
