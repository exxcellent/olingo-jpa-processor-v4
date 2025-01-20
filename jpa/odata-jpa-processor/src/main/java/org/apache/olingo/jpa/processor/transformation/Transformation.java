package org.apache.olingo.jpa.processor.transformation;

import org.apache.olingo.jpa.processor.transformation.impl.TransformationSequence;
import org.apache.olingo.server.api.serializer.SerializerException;

import javax.validation.constraints.NotNull;

/**
 * Every implementing class must have an default constructor.
 */
public interface Transformation<Input, Output> {

  /**
   * Runtime helper method to work with the input class type outside of compiler generics.
   * @return The class type of input for transformation.
   */
  public @NotNull Class<Input> getInputType();

  /**
   * Runtime helper method to work with the output class type outside of compiler generics.
   * @return The class type of output for transformation.
   */
  public @NotNull Class<Output> getOutputType();

  public Output transform(@NotNull Input input) throws SerializerException;

  /**
   * Convert this transformation into a chainable representation.
   */
  @SuppressWarnings("unchecked")
  public default @NotNull TransformationChain<Input, Output> asChain() {
    if(TransformationChain.class.isInstance(this)) {
      return (TransformationChain<Input, Output>) TransformationChain.class.cast(this);
    } else {
      return new TransformationSequence<Input, Output>(this);
    }
  }  
}
