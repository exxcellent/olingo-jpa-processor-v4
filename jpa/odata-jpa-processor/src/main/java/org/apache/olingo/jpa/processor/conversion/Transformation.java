package org.apache.olingo.jpa.processor.conversion;

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
}
