package org.apache.olingo.jpa.processor.conversion;

/**
 * An instance of this class describes the target of an transformation.
 */
public abstract class AbstractTransformationDescription<Input, Output> {

  private final Class<Input> inputType;
  private final Class<Output> outputType;

  /**
   *
   * @param outputType The output type of {@link Transformation}.
   */
  protected AbstractTransformationDescription(final Class<Input> inputType, final Class<Output> outputType) {
    this.inputType = inputType;
    this.outputType = outputType;
  }

  public Class<Input> getInputType() {
    return inputType;
  }

  public Class<Output> getOutputType() {
    return outputType;
  }

}
