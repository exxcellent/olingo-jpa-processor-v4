package org.apache.olingo.jpa.processor.transformation;

import org.apache.olingo.server.api.serializer.SerializerException;

/**
 * Every implementing class must have an default constructor.
 */
public interface Transformation<Input, Output> {

  /**
   * Runtime helper method to work with the input class type outside of compiler generics.
   * @return The class type of input for transformation.
   */
  public Class<Input> getInputType();

  /**
   * Runtime helper method to work with the output class type outside of compiler generics.
   * @return The class type of output for transformation.
   */
  public Class<Output> getOutputType();

  public Output transform(Input input) throws SerializerException;

  /**
   * Not explicit part of transformation api, but common use case: a transformation consist of multiple steps converting
   * from <i>Input</i> to <i>Output</i> via several intermediate formats. Often such pipeline can be break open to build
   * sub pipeline of transforming starting not with first step, but with a step in between.
   *
   * @param newStart The new input format. Maybe the same as the original <i>Input</i>... then simply return the
   * existing transformation.
   * @return The new sub transformation transforming the changed input to the target output format.
   * @throws SerializerException If sub transformation is not possible.
   */
  public <I> Transformation<I, Output> createSubTransformation(Class<I> newStart) throws SerializerException;
}
