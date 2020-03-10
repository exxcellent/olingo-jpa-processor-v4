package org.apache.olingo.jpa.processor.conversion;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * An instance of this class describes the declaration of an transformation.
 */
public final class TransformationDeclaration<Input, Output> extends AbstractTransformationDescription<Input, Output> {

  public final static class Builder<Input, Output> {
    private final TransformationDeclaration<Input, Output> descriptor;

    public Builder(final Class<Input> inputType, final Class<Output> outputType) {
      this.descriptor = new TransformationDeclaration<>(inputType, outputType);
    }

    Builder<Input, Output> addConstraint(final Comparable<?> constraint) {
      descriptor.addConstraint(constraint);
      return this;
    }

    /**
     *
     * @param alternatives Only one of the alternatives must match
     */
    Builder<Input, Output> addConstraintAlternatives(final Comparable<?>... alternatives) {
      descriptor.addConstraintAlternatives(alternatives);
      return this;
    }

    public TransformationDeclaration<Input, Output> build() {
      return descriptor;
    }
  }

  private static class Constraint {
    private final List<Comparable<?>> alternatives;

    private Constraint(final Comparable<?>... alternatives) {
      this.alternatives = Arrays.asList(alternatives);
    }

    @Override
    public String toString() {
      return "Constraint [" + (alternatives != null ? "alternatives=" + alternatives : "") + "]";
    }

  }

  private final List<Constraint> constraints = new LinkedList<Constraint>();

  /**
   *
   * @param outputType The output type of {@link Transformation}.
   * @param constraints Optional list of constraints. The order of constraints must match the expectations of
   * transformation (order of registration must be the same as order of usage request).
   */
  private TransformationDeclaration(final Class<Input> inputType, final Class<Output> outputType) {
    super(inputType, outputType);
  }

  private void addConstraint(final Comparable<?> constraint) {
    addConstraintAlternatives(constraint);
  }

  /**
   *
   * @param alternatives One of the given elements must be matched by transformation
   */
  private void addConstraintAlternatives(final Comparable<?>... alternatives) {
    if (alternatives == null || alternatives.length < 1) {
      throw new IllegalArgumentException("At least one element required");
    }
    constraints.add(new Constraint(alternatives));
  }

  public int getNumberOfConstraints() {
    return constraints.size();
  }

  private final boolean hasMatchingTypes(final AbstractTransformationDescription<?, ?> other) {
    if (!other.getInputType().isAssignableFrom(this.getInputType())) {
      return false;
    }
    if (!other.getOutputType().isAssignableFrom(this.getOutputType())) {
      return false;
    }
    return true;
  }

  public boolean isMatching(final TransformationDeclaration<?, ?> other) {
    if (!hasMatchingTypes(other)) {
      return false;
    }
    if (this.getNumberOfConstraints() != other.getNumberOfConstraints()) {
      return false;
    }
    for (int i = 0; i < constraints.size(); i++) {
      final Constraint cThis = this.constraints.get(i);
      final Constraint cOther = this.constraints.get(i);
      if (!hasAnyDuplicatedEntry(cThis, cOther)) {
        return false;
      }
    }
    return true;
  }

  private boolean hasAnyDuplicatedEntry(final Constraint cThis, final Constraint cOther) {
    for (int i = 0; i < cThis.alternatives.size(); i++) {
      final Comparable<?> entryThis = cThis.alternatives.get(i);
      if (cOther.alternatives.contains(entryThis)) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @return TRUE if this descriptor is matching at least all the input/out/constraints or having more constraints than
   * the requested descriptor (means: this descriptor is more specialized).
   */
  public boolean isMatching(final TransformationRequest<?, ?> request) {
    if (request == null) {
      return false;
    }
    if (!hasMatchingTypes(request)) {
      return false;
    }
    // accept only exactly the same number of constraints
    if (getNumberOfConstraints() != request.getNumberOfConstraints()) {
      return false;
    }

    final List<Comparable<?>> constraintsOther = request.getConstraints();
    for (int i = 0; i < constraints.size(); i++) {
      final Comparable<?> oOther = constraintsOther.get(i);
      if (!this.constraints.get(i).alternatives.contains(oOther)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "TransformationDeclaration [" + (constraints != null ? "constraints=" + constraints + ", " : "")
        + (getInputType() != null ? "getInputType()=" + getInputType() + ", " : "") + (getOutputType() != null
        ? "getOutputType()=" + getOutputType() : "") + "]";
  }



}
